import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.print.DocFlavor.STRING;

public class PeerInfo implements Serializable {
	private File peerDir;
	private File backupDir;
	private Integer storageCapacity;
	private List<FileInfo> filesInfo = new ArrayList<FileInfo>();
	private List<ChunkInfo> chunksInfo = new ArrayList<ChunkInfo>();
	private Map<Integer, List<String>> toDelete = new ConcurrentHashMap<Integer, List<String>>();

	public PeerInfo(int id) {
		this.peerDir = new File("../../peers/" + String.valueOf(id) + "/");
		this.backupDir = new File(this.peerDir.getPath() + "/backup/");
		this.storageCapacity = 999999999; // 999 Gbytes
		if (!backupDir.exists()) {
			backupDir.mkdirs();
		}
	}

	public File getBackupDir() {
		return backupDir;
	}

	public File getPeerDir() {
		return peerDir;
	}

	public List<FileInfo> getFilesInfo() {
		return filesInfo;
	}

	public void addToDelete(Integer peerId, String fileId) {
		if (toDelete.containsKey(peerId)) {
			List<String> filesId = toDelete.get(peerId);
			if (!filesId.contains(fileId))
				filesId.add(fileId);
		} else {
			List<String> filesId = new ArrayList<String>();
			filesId.add(fileId);
			toDelete.put(peerId, filesId);
		}
	}

	public void removeToDelete(Integer peerId, String fileId) {
		if (!toDelete.containsKey(peerId))
			return;
		List<String> filesId = toDelete.get(peerId);
		for (int i = 0; i < filesId.size(); i++) {
			if (filesId.get(i).equals(fileId)) {
				filesId.remove(i--);
			}
		}
		if (filesId.size() == 0)
			toDelete.remove(peerId);
	}

	public List<String> getToDelete(Integer peerId) {
		return toDelete.get(peerId);
	}

	public FileInfo getFileInfo(String path) {
		for (int i = 0; i < filesInfo.size(); i++) {
			if (filesInfo.get(i).getPath().equals(path)) {
				return filesInfo.get(i);
			}
		}
		return null;
	}

	public void removeFileInfo(String path) {
		for (int i = 0; i < filesInfo.size(); i++) {
			if (filesInfo.get(i).getPath().equals(path)) {
				filesInfo.remove(i);
				break;
			}
		}
	}

	public void addFile(String path, String id, int desiredReplication) {
		for (FileInfo fileInfo: filesInfo) {
			if (fileInfo.getPath().equals(path))
				return;
		}
		this.filesInfo.add(new FileInfo(path, id, desiredReplication));
	}

	public void addFileChunk(String filePath, int chunkNo, int size) {
		for (int i = 0; i < filesInfo.size(); i++) {
			if (filesInfo.get(i).getPath().equals(filePath)) {
				filesInfo.get(i).addChunk(chunkNo, size);
				break;
			}
		}
	}

	public List<Integer> getReplicationDegs(String id) {
		for (int i = 0; i < chunksInfo.size(); i++) {
			if (chunksInfo.get(i).getId().equals(id)) {
				return chunksInfo.get(i).getReplicationDegree();
			}
		}
		return null;
	}

	public List<Integer> getReplicationDegs(String fileId, String chunkId) {
		for (int i = 0; i < filesInfo.size(); i++) {
			if (filesInfo.get(i).getId().equals(fileId)) {
				return filesInfo.get(i).getReplicationDegree(chunkId);
			}
		}
		return null;
	}

	public void addChunk(String id, int chunkNo, int size, int desiredReplication) {
		this.chunksInfo.add(new ChunkInfo(id, chunkNo, size, desiredReplication));
	}

	public ChunkInfo getChunk(String id) {
		for (int i = 0; i < chunksInfo.size(); i++) {
			if (chunksInfo.get(i).getId().equals(id))
				return chunksInfo.get(i);
		}
		return null;
	}

	public List<ChunkInfo> getChunksInfo() {
		return chunksInfo;
	}

	public FileInfo getFileInfoById(String id) {
		for (int i = 0; i < filesInfo.size(); i++) {
			if (filesInfo.get(i).getId().equals(id)) {
				return filesInfo.get(i);
			}
		}
		return null;
	}

	public Integer getCapacity() {
		return storageCapacity;
	}

	public void setCapacity(Integer capacity) {
		storageCapacity = capacity;
	}

	public Integer calculateUsedStorage() {
		int usedStorage = 0;
		for (int i = 0; i < chunksInfo.size(); i++) {
			usedStorage += chunksInfo.get(i).getSize();
		}
		return usedStorage;
	}

	/**
	 * Removes the first chunks that have more perceived replication than
	 * desired replication until removed at lease space bytes
	 * @param space space to be removed
	 * @param force true if it really has to get the space, and false if it's only a possibility
	 * 		(usefull to use when saving a chunk but has no space)
	 * @return list of all the ChunkInfos that were removed
	 */
	public List<ChunkInfo> emptySpace(int space, boolean force) {
		int initialUsedStorage = calculateUsedStorage();
		List<ChunkInfo> removedChunks = new ArrayList<ChunkInfo>();
		for (int i = 0; i < chunksInfo.size(); i++) {
			if (chunksInfo.get(i).getPerceivedReplication() > chunksInfo.get(i).getDesiredReplication()) {
				if (this.removeChunk(chunksInfo.get(i))) {
					removedChunks.add(chunksInfo.get(i));
					this.chunksInfo.remove(i--);
				}
				if (initialUsedStorage - calculateUsedStorage() > space)
					return removedChunks;
			}
		}
		if (force) {
			for (int i = 0; i < chunksInfo.size(); i++) {
				if (this.removeChunk(chunksInfo.get(i))) {
					removedChunks.add(chunksInfo.get(i));
					this.chunksInfo.remove(i--);
				}
				if (initialUsedStorage - calculateUsedStorage() > space)
					return removedChunks;
			}
		}
		return removedChunks;
	}

	public boolean removeChunk(ChunkInfo chunkInfo) {
		File chunkFile = new File(backupDir + "/" + chunkInfo.getId());
		if (chunkFile.delete()) {
			return true;
		}
		return false;
	}

	public void state(String prefix) {
		if (filesInfo.size() != 0)
			System.out.println(prefix + "-- Backup files --");
		for (int i = 0; i < filesInfo.size(); i++) {
			filesInfo.get(i).state(prefix + "  ");
			System.out.println("");
		}
		System.out.println("");
		if (chunksInfo.size() != 0)
			System.out.println(prefix + "-- Chunks --");
		for (int i = 0; i < chunksInfo.size(); i++) {
			chunksInfo.get(i).state(prefix + "  ");
			System.out.println("");
		}
		System.out.println("");
		System.out.println("Storage capacity: " + calculateUsedStorage() + "/" + storageCapacity + " Kbytes");
		System.out.println("");
	}
}
