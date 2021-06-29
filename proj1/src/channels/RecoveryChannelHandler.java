import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;

public class RecoveryChannelHandler extends MulticastChannelHandler {
	public RecoveryChannelHandler(Peer peer, DatagramPacket packet) {
		super(peer, packet);
	}

	public void run() {
		byte[] message = new byte[receivedPacket.getLength()];
		message = Arrays.copyOf(receivedPacket.getData(), message.length);

		this.handle(peer, message);
	}

	public static void handle(Peer peer, byte[] message) {
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
		if (messageHeader.getType() == MessageType.CHUNK) {
			chunk(peer, body, messageHeader);
		}
	}


	public static void chunk(Peer peer, byte[] body, Header messageHeader) {
		String chunkId = messageHeader.getFileId() + messageHeader.getChunkNo();
		if (peer.chunkToReceive.equals(chunkId) && !peer.getLastReceivedChunkId().equals(chunkId)) {
			FileInfo fileInfo = peer.getFileInfoById(messageHeader.getFileId());
			String path = fileInfo.getPath();
			try {
				File file = new File(peer.getPeerDir() + "/" + path);
				if (!file.exists()) {
					file.createNewFile();
				}
				peer.setLastReceivedChunkId(messageHeader.getFileId() + messageHeader.getChunkNo());
				FileOutputStream outputStream = new FileOutputStream(file, true);
				outputStream.write(body);
				outputStream.close();
				peer.receivedChunk = true;
			} catch (Exception e ) {
				e.printStackTrace();
			}
		} else {
			peer.setLastReceivedChunkId(messageHeader.getFileId() + messageHeader.getChunkNo());
		}
	}

}
