import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.io.File;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.Socket;

public class ControlChannelHandler extends MulticastChannelHandler {
	public ControlChannelHandler(Peer peer, DatagramPacket packet) {
		super(peer, packet);
	}

	public void run() {
		String message = new String(receivedPacket.getData(), 0, receivedPacket.getLength());

		String[] messageParts = message.split(new String(Header.CRLF));
		Header messageHeader = new Header(messageParts[0]);

		switch (messageHeader.getType()) {
		case STORED:
			this.stored(messageParts, messageHeader);
			break;
		case DELETE:
			this.delete(messageParts, messageHeader);
			break;
		case GETCHUNK:
			this.restore(messageParts, messageHeader);
			break;
		case REMOVED:
			this.removed(messageHeader);
			break;
		case DELETED:
			this.deleted(messageHeader);
			break;
		case ON:
			this.on(messageHeader);
			break;
		default:
			System.out.println("Control channel received an unrecognized message");
		}
	}

	private void stored(String[] messageParts, Header messageHeader) {
		String chunkId = messageHeader.getFileId() + String.valueOf(messageHeader.getChunkNo());
		ChunkInfo chunk = this.peer.getChunk(chunkId);
		if (chunk == null) {
			FileInfo fileInfo = this.peer.getFileInfoById(messageHeader.getFileId());
			if (fileInfo != null)
				chunk = fileInfo.getChunk(chunkId);
		}
		if (chunk != null) {
			chunk.addReplication(messageHeader.getSenderId());
		} else {
			this.peer.addChunk(messageHeader.getFileId(), messageHeader.getChunkNo(), -1, 10);
			chunk = this.peer.getChunk(chunkId);
			chunk.addReplication(messageHeader.getSenderId());
		}
	}

	private void delete(String[] messageParts, Header messageHeader) {
		List<ChunkInfo> chunksInfo = this.peer.getChunksInfo();
		File backupDir = this.peer.getBackupDir();
		for (int i = 0; i < chunksInfo.size(); i++) {
			if (chunksInfo.get(i).getId().startsWith(messageHeader.getFileId())) {
				File file = new File(backupDir.getPath() + "/" + chunksInfo.get(i).getId());
				try {
					file.delete();
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (this.peer.getVersion() > 1.0) {
					List<Integer> replications = chunksInfo.get(i).getReplicationDegree();
					for (Integer peer: replications) {
						this.peer.addToDelete(peer, messageHeader.getFileId());
					}
				}
				chunksInfo.remove(i--);
			}
		}

		if (this.peer.getVersion() > 1.0) {
			Integer time = new Random().nextInt(400);
			try {
				Thread.sleep(time);
			} catch (Exception e) {
				e.printStackTrace();
			}
			Header responseHeader = new Header(this.peer.getVersion(), MessageType.DELETED,
												 this.peer.getId(), messageHeader.getFileId());
			this.peer.getMC().send(responseHeader.getHeader().getBytes());
		}
	}

	private void deleted(Header messageHeader) {
		this.peer.removeToDelete(messageHeader.getSenderId(), messageHeader.getFileId());
	}

	private void on(Header messageHeader) {
		List<String> toDelete = this.peer.getToDelete(messageHeader.getSenderId());
		if (toDelete == null) return;
		for (String fileId: toDelete) {
			Integer time = new Random().nextInt(400);
			try {
				Thread.sleep(time);
			} catch (Exception e) {
				e.printStackTrace();
			}
			Header responseHeader = new Header(this.peer.getVersion(), MessageType.DELETE, this.peer.getId(), fileId);
			this.peer.getMC().send(responseHeader.getHeader().getBytes());
		}
	}

	private void restore(String[] messageParts, Header messageHeader) {
		File backupDir = this.peer.getBackupDir();
		String chunkId = messageHeader.getFileId() + messageHeader.getChunkNo();
		ChunkInfo chunkInfo = this.peer.getChunk(chunkId);
		if (chunkInfo == null || chunkInfo.getSize() == -1 || this.peer.getId() == messageHeader.getSenderId())
			return;
		Header responseHeader = new Header(this.peer.getVersion(), MessageType.CHUNK, this.peer.getId(),
				messageHeader.getFileId(), messageHeader.getChunkNo());
		byte[] header = responseHeader.getHeader().getBytes();
		this.peer.setLastReceivedChunkId("");

		try {
			byte[] body = Files.readAllBytes(Paths.get(this.peer.getBackupDir() + "/" + chunkId));
			byte[] message = Arrays.copyOf(header, header.length + body.length);
			System.arraycopy(body, 0, message, header.length, body.length);
			Integer time = new Random().nextInt(400);
			try {
				Thread.sleep(time);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (!this.peer.getLastReceivedChunkId().equals(chunkId)) {
				if (this.peer.getVersion() > 1.2)
					this.respondTCP(messageHeader, message);
				else
					this.peer.getMDR().send(message);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void respondTCP(Header messageHeader, byte[] message) {
		try {
			int portNumber = messageHeader.getPort();
			InetAddress address = InetAddress.getByName(messageHeader.getAddress());
			Socket socket = new Socket(address, portNumber);
			OutputStream outputStream = socket.getOutputStream();

			outputStream.write(message, 0, message.length);

			socket.shutdownOutput();
		} catch (Exception e) {
			// Because another peer is already responding
		}
	}

	private void removed(Header messageHeader) {
		if (messageHeader.getSenderId() == this.peer.getId()) // Not peer who sent message
			return;

		String chunkId = messageHeader.getFileId() + messageHeader.getChunkNo();
		ChunkInfo chunk = this.peer.getChunk(chunkId);
		if (chunk == null) {
			FileInfo fileInfo = this.peer.getFileInfoById(messageHeader.getFileId());
			if (fileInfo != null) {
				chunk = fileInfo.getChunk(chunkId);
				chunk.subReplication(messageHeader.getSenderId());
			}
			return;
		}
		if (chunk == null)
			return;
		chunk.subReplication(messageHeader.getSenderId());

		if (chunk.getPerceivedReplication() >= chunk.getDesiredReplication())
			return;

		this.peer.setLastReceivedChunkId("");

		Integer time = new Random().nextInt(400);
		try {
			Thread.sleep(time);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			Header responseHeader = new Header(this.peer.getVersion(), MessageType.PUTCHUNK, this.peer.getId(),
					chunk.getFileId(), chunk.getChunkNo(), chunk.getDesiredReplication());
			byte[] header = responseHeader.getHeader().getBytes();
			byte[] body = Files.readAllBytes(Paths.get(this.peer.getBackupDir() + "/" + chunkId));
			byte[] message = Arrays.copyOf(header, header.length + body.length);
			System.arraycopy(body, 0, message, header.length, body.length);
			if (chunkId == this.peer.getLastReceivedChunkId())
				return;
			this.peer.getMDB().send(message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
