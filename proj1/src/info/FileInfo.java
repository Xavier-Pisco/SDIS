import java.io.Serializable;
import java.util.*;

public class FileInfo implements Serializable {
	String path;
	String id;
	int replicationDegree;
	List<ChunkInfo> chunksInfo = new ArrayList<ChunkInfo>();

	public FileInfo(String path, String id, int replicationDegree) {
		this.path = path;
		this.id = id;
		this.replicationDegree = replicationDegree;
	}

	public String getPath() {
		return path;
	}

	public String getId() {
		return id;
	}

	public int getDesiredReplications() {
		return replicationDegree;
	}

	public List<ChunkInfo> getChunksInfo() {
		return chunksInfo;
	}

	public ChunkInfo getChunk(String id) {
		for (int i = 0; i < chunksInfo.size(); i++) {
			if (chunksInfo.get(i).getId().equals(id)) {
				return chunksInfo.get(i);
			}
		}
		return null;
	}

	public List<Integer> getReplicationDegree(String id) {
		for (int i = 0; i < chunksInfo.size(); i++) {
			if (chunksInfo.get(i).getId().equals(id)) {
				return chunksInfo.get(i).getReplicationDegree();
			}
		}
		return null;
	}

	public void addChunk(int chunkNo, int size) {
		this.chunksInfo.add(new ChunkInfo(this.id, chunkNo, size, this.replicationDegree));
	}

	public void state(String prefix) {
		System.out.println(prefix + "Path: " + path);
		System.out.println(prefix + "Id: " + id);
		System.out.println(prefix + "Desired replication: " + replicationDegree);
		System.out.println("");
		for (int i = 0; i < chunksInfo.size(); i++) {
			chunksInfo.get(i).state(prefix + "  ");
			System.out.println("");
		}
	}
}
