import java.net.InetAddress;

enum MessageType {
	PUTCHUNK, STORED, GETCHUNK, CHUNK, DELETE, REMOVED, DELETED, ON
};

public class Header {
	public static byte[] CRLF = {0x0D, 0x0A};
	Double version;
	MessageType type;
	int senderId;
	String fileId;
	int chunkNo;
	int replicationDeg;
	String address;
	int port;

	public Header(Double version, MessageType type, int senderId, String fileId, int chunkNo, String address, int port) {
		this.version = version;
		this.type = type;
		this.senderId = senderId;
		this.fileId = fileId;
		this.chunkNo = chunkNo;
		this.address = address;
		this.port = port;
	}

	public Header(Double version, MessageType type, int senderId, String fileId, int chunkNo, int replicationDeg) {
		this.version = version;
		this.type = type;
		this.senderId = senderId;
		this.fileId = fileId;
		this.chunkNo = chunkNo;
		this.replicationDeg = replicationDeg;
	}

	public Header(Double version, MessageType type, int senderId, String fileId, int chunkNo) {
		this.version = version;
		this.type = type;
		this.senderId = senderId;
		this.fileId = fileId;
		this.chunkNo = chunkNo;
	}

	public Header(Double version, MessageType type, int senderId, String fileId) {
		this.version = version;
		this.type = type;
		this.senderId = senderId;
		this.fileId = fileId;
	}

	public Header(Double version, MessageType type, int senderId) {
		this.version = version;
		this.type = type;
		this.senderId = senderId;
	}

	public Header(String header) {
		String[] headerParts = header.split(" +");
		this.version = Double.parseDouble(headerParts[0]);
		this.senderId = Integer.parseInt(headerParts[2]);
		switch (headerParts[1]) {
			case "PUTCHUNK":
				this.type = MessageType.PUTCHUNK;
				this.fileId = headerParts[3];
				this.chunkNo = Integer.parseInt(headerParts[4]);
				this.replicationDeg = Integer.parseInt(headerParts[5]);
				break;
			case "STORED":
				this.type = MessageType.STORED;
				this.fileId = headerParts[3];
				this.chunkNo = Integer.parseInt(headerParts[4]);
				break;
			case "GETCHUNK":
				this.type = MessageType.GETCHUNK;
				this.fileId = headerParts[3];
				this.chunkNo = Integer.parseInt(headerParts[4]);
				if (this.version > 1.2) {
					this.address = headerParts[5];
					this.port = Integer.parseInt(headerParts[6]);
				}
				break;
			case "CHUNK":
				this.type = MessageType.CHUNK;
				this.fileId = headerParts[3];
				this.chunkNo = Integer.parseInt(headerParts[4]);
				break;
			case "DELETE":
				this.type = MessageType.DELETE;
				this.fileId = headerParts[3];
				break;
			case "REMOVED":
				this.type = MessageType.REMOVED;
				this.fileId = headerParts[3];
				this.chunkNo = Integer.parseInt(headerParts[4]);
				break;
			case "DELETED":
				this.type = MessageType.DELETED;
				this.fileId = headerParts[3];
				break;
			case "ON":
				this.type = MessageType.ON;
				break;
			default:
				System.out.println("Uknown message type");
				break;
		}
	}

	public String getHeader() {
		StringBuilder message = new StringBuilder(String.valueOf(version));
		switch(this.type) {
			case PUTCHUNK:
				message.append(" PUTCHUNK ");
				message.append(String.valueOf(this.senderId) + " " + fileId + " " + String.valueOf(this.chunkNo) + " " + String.valueOf(this.replicationDeg) + new String(CRLF) + new String(CRLF));
				break;
			case STORED:
				message.append(" STORED ");
				message.append(String.valueOf(this.senderId) + " " + fileId + " " + String.valueOf(this.chunkNo) + new String(CRLF) + new String(CRLF));
				break;
			case GETCHUNK:
				message.append(" GETCHUNK ");
				message.append(String.valueOf(this.senderId) + " " + fileId + " " + String.valueOf(this.chunkNo));
				if (version > 1.2)
					message.append(" " + address + " " + String.valueOf(port));
				message.append(new String(CRLF) + new String(CRLF));
				break;
			case CHUNK:
				message.append(" CHUNK ");
				message.append(String.valueOf(this.senderId) + " " + fileId + " " + String.valueOf(this.chunkNo) + new String(CRLF) + new String(CRLF));
				break;
			case DELETE:
				message.append(" DELETE ");
				message.append(String.valueOf(this.senderId) + " " + fileId + new String(CRLF) + new String(CRLF));
				break;
			case REMOVED:
				message.append(" REMOVED ");
				message.append(String.valueOf(this.senderId) + " " + fileId + " " + String.valueOf(this.chunkNo) + new String(CRLF) + new String(CRLF));
				break;
			case DELETED:
				message.append(" DELETED ");
				message.append(String.valueOf(this.senderId) + " " + fileId + new String(CRLF) + new String(CRLF));
				break;
			case ON:
				message.append(" ON ");
				message.append(String.valueOf(this.senderId) + new String(CRLF) + new String(CRLF));
				break;
			default:
				break;
		}
		return message.toString();
	}

	public MessageType getType() {
		return this.type;
	}

	public Double getVersion() {
		return this.version;
	}

	public int getSenderId() {
		return this.senderId;
	}

	public String getFileId() {
		return this.fileId;
	}

	public int getChunkNo() {
		return this.chunkNo;
	}

	public int getReplicationDegree() {
		return this.replicationDeg;
	}

	public String getAddress() {
		return this.address;
	}

	public int getPort() {
		return this.port;
	}
}
