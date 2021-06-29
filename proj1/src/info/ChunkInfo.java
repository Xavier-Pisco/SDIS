import java.io.*;
import java.util.*;

public class ChunkInfo implements Serializable {
	String fileId;
	int chunkNo;
	int size; // in Kbytes
	int desiredReplication;
	List<Integer> replicationDegs = new ArrayList<Integer>();

	public ChunkInfo(String id, int chunkNo, int size, int desiredReplication) {
		this.fileId = id;
		this.chunkNo = chunkNo;
		this.size = size;
		this.desiredReplication = desiredReplication;
	}

	public String getId() {
		return fileId + chunkNo;
	}
	public String getFileId() {
		return fileId;
	}
	public Integer getChunkNo() {
		return chunkNo;
	}
	public int getSize() {
		return size;
	}
	public int getDesiredReplication() {
		return desiredReplication;
	}
	public List<Integer> getReplicationDegree() {
		return replicationDegs;
	}
	public int getPerceivedReplication() {
		return replicationDegs.size();
	}
	public void addReplication(Integer peerId) {
		if (!this.replicationDegs.contains(peerId))
			this.replicationDegs.add(peerId);
	}
	public void subReplication(Integer peerId) {
		replicationDegs.remove(peerId);
	}
	public void setSize(int size) {
		this.size = size;
	}
	public void setDesiredReplication(int dr) {
		this.desiredReplication = dr;
	}

	public void state(String prefix) {
		if (size != -1) {
			System.out.println(prefix + "Id: " + fileId + chunkNo);
			System.out.println(prefix + "Size: " + size + "Kbytes");
			System.out.println(prefix + "Desired Replication: " + desiredReplication);
			System.out.println(prefix + "Perceived Replication: " + replicationDegs.size());
		}
	}
}
