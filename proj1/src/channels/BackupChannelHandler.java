import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;

public class BackupChannelHandler extends MulticastChannelHandler {
	public BackupChannelHandler(Peer peer, DatagramPacket packet) {
		super(peer, packet);
	}

	public void run() {
		byte[] message = new byte[receivedPacket.getLength()];
		message = Arrays.copyOf(receivedPacket.getData(), message.length);

		int i = 0;
		while (i < message.length - 4) {
			if (Header.CRLF[0] == message[i] && Header.CRLF[1] == message[i+1])
				if (Header.CRLF[0] == message[i+2] && Header.CRLF[1] == message[i+3]) {
					break;
				}
			i++;
		}

		int j = i+4; //Skip both \n
		byte[] header = new byte[i];
		byte[] body = new byte[message.length - j];
		System.arraycopy(message, 0, header, 0, i);
		System.arraycopy(message, j, body, 0, message.length - j);

		Header messageHeader = new Header(new String(header));

		if (messageHeader.getSenderId() == this.peer.getId()) { // Not the peer who sent the message
			return;
		}
		if (this.peer.getFileInfoById(messageHeader.getFileId()) != null) // Not peer who initiated file backup
			return;

		Integer time = new Random().nextInt(400);
		try {
			Thread.sleep(time);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (messageHeader.getType() == MessageType.PUTCHUNK) {
			String chunkId = messageHeader.getFileId() + String.valueOf(messageHeader.getChunkNo());
			ChunkInfo chunk = this.peer.getChunk(chunkId);
			if (chunk == null) {
				this.peer.addChunk(messageHeader.getFileId(), messageHeader.getChunkNo(), body.length / 1000, messageHeader.getReplicationDegree());
				chunk = this.peer.getChunk(chunkId);
			} else {
				chunk.setDesiredReplication(messageHeader.getReplicationDegree());
			}
			if (putChunk(body, messageHeader, chunk)) {
				Header responseHeader = new Header(this.peer.getVersion(), MessageType.STORED, this.peer.getId(), messageHeader.getFileId(),
				messageHeader.getChunkNo());
				chunk.setSize(body.length / 1000);
				this.peer.setLastReceivedChunkId(messageHeader.getFileId() + messageHeader.getChunkNo());
				this.peer.getMC().send(responseHeader.getHeader().getBytes());
			}
		}
	}

	private boolean putChunk(byte[] body, Header messageHeader, ChunkInfo chunk) {
		if (this.peer.getRemainingSpace() < body.length/1000) {
			if (!this.peer.getSpace(body.length / 1000 - this.peer.getRemainingSpace(), false))
				return false;
		}

		if (this.peer.getVersion() > 1.1){
			if (chunk.getDesiredReplication() <= chunk.getPerceivedReplication()) {
				return false;
			}
		}

		try {

			File backupDir = peer.getBackupDir();
			File backupFile = new File(
					backupDir.getPath() + "/" + messageHeader.getFileId() + String.valueOf(messageHeader.getChunkNo()));

			if (backupFile.exists()) {
				return true;
			} else {
				new File(backupFile.getParent()).mkdirs();
			}

			backupFile.createNewFile();

			FileOutputStream outputStream = new FileOutputStream(backupFile);
			outputStream.write(body);
			outputStream.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
}
