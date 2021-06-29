import java.io.*;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.net.*;
import java.rmi.registry.*;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;

public class Peer implements RMIStub {
	public final int BLOCK_SIZE = 64000;
	private String testFolder = "../../test/";
	private Double version;
	private int id;
	private ControlChannel MC;
	private BackupChannel MDB;
	private RecoveryChannel MDR;
	private String accessPoint;
	private PeerInfo peerInfo;
	private File infoFile;
	private String lastReceivedChunkId = "";
	public String chunkToReceive = "";
	public boolean receivedChunk = false;

	private ScheduledThreadPoolExecutor executor;

	public Peer(Double version, int id, String accessPoint, String MCAddress, int MCPort, String MDBAddress,
			int MDBPort, String MDRAddress, int MDRPort) {
		this.version = version;
		this.id = id;
		this.accessPoint = accessPoint;
		this.peerInfo = new PeerInfo(id);
		this.infoFile = new File(peerInfo.getPeerDir().getPath() + "/info");
		try {
			if (!infoFile.exists()) {
				infoFile.createNewFile();
				this.saveInfo();
			} else {
				ObjectInputStream in = new ObjectInputStream(new FileInputStream(infoFile));
				this.peerInfo = (PeerInfo) in.readObject();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			this.MC = new ControlChannel(MCAddress, MCPort, this);
			this.MDB = new BackupChannel(MDBAddress, MDBPort, this);
			this.MDR = new RecoveryChannel(MDRAddress, MDRPort, this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.executor = new ScheduledThreadPoolExecutor(100);
	}

	/**
	 * Main function for a peer
	 */
	public void run() {
		// Now only waits to read on all three channels
		executor.execute(MC);
		executor.execute(MDB);
		executor.execute(MDR);
		if (this.version > 1.0) {
			Header onMessage = new Header(version, MessageType.ON, id);
			MC.send(onMessage.getHeader().getBytes());
		}
	}

	public Double getVersion() {
		return this.version;
	}

	public int getId() {
		return this.id;
	}

	public File getBackupDir() {
		return this.peerInfo.getBackupDir();
	}

	public File getPeerDir() {
		return this.peerInfo.getPeerDir();
	}

	public ControlChannel getMC() {
		return this.MC;
	}

	public BackupChannel getMDB() {
		return this.MDB;
	}

	public RecoveryChannel getMDR() {
		return this.MDR;
	}

	public ScheduledThreadPoolExecutor getExecutor() {
		return this.executor;
	}

	public List<Integer> getChunkReplication(String id) {
		return peerInfo.getReplicationDegs(id);
	}

	public List<Integer> getFileChunkReplication(String fileId, String chunkId) {
		return peerInfo.getReplicationDegs(fileId, chunkId);
	}

	public void addChunk(String id, int chunkNo, int size, int desiredReplication) {
		this.peerInfo.addChunk(id, chunkNo, size, desiredReplication);
	}

	public ChunkInfo getChunk(String id) {
		return this.peerInfo.getChunk(id);
	}

	public List<ChunkInfo> getChunksInfo() {
		return peerInfo.getChunksInfo();
	}

	public FileInfo getFileInfoById(String id) {
		return peerInfo.getFileInfoById(id);
	}

	public String getLastReceivedChunkId() {
		return lastReceivedChunkId;
	}

	public void setLastReceivedChunkId(String chunkId) {
		this.lastReceivedChunkId = chunkId;
	}

	public void addToDelete(Integer peerId, String fileId) {
		this.peerInfo.addToDelete(peerId, fileId);
	}

	public void removeToDelete(Integer peerId, String fileId) {
		this.peerInfo.removeToDelete(peerId, fileId);
	}

	public List<String> getToDelete(Integer peerId) {
		return this.peerInfo.getToDelete(peerId);
	}

	public void saveInfo() {
		try {
			FileOutputStream fout = new FileOutputStream(infoFile);
			ObjectOutputStream out = new ObjectOutputStream(fout);
			out.writeObject(peerInfo);
			out.flush();
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}

	public void backup(String fileName, int replicationDegree) throws RemoteException {
		if (replicationDegree > 9) {
			System.out.println("Replication degree must be less or equal to 9");
			return;
		}
		String path = testFolder + fileName;
		byte[] bytes;
		try {
			bytes = Files.readAllBytes(Paths.get(path));
			if (bytes.length > 64000000000l) {
				System.out.println("File is too big. Max size is 64GBytes");
				return;
			}
		} catch (Exception e) {
			System.out.println("File does not exist in folder: " + testFolder);
			return;
		}
		try {
			String codedPath = hash(fileName, path);
			int initial_byte = 0;
			int final_byte = Math.min(BLOCK_SIZE, bytes.length);
			int i = 0;
			if (peerInfo.getFileInfo(fileName) != null) {
				String savedId = peerInfo.getFileInfo(fileName).getId();
				if (!codedPath.equals(savedId)) {
					System.out.println("\nYou already have a backup from an older version of this file.\nIf you are sure about what you're doing delete that backup and create another one.");
					return;
				}
			}
			peerInfo.addFile(fileName, codedPath, replicationDegree);
			do {
				final_byte = initial_byte + Math.min(BLOCK_SIZE, bytes.length - initial_byte);
				byte[] header = (new Header(this.version, MessageType.PUTCHUNK, this.id, codedPath, i,
						replicationDegree)).getHeader().getBytes();
				byte[] body = Arrays.copyOfRange(bytes, initial_byte, final_byte);

				byte[] message = Arrays.copyOf(header, header.length + body.length);
				System.arraycopy(body, 0, message, header.length, body.length);

				System.out.println("Sending chunk nº" + String.valueOf(i));

				String chunkId = codedPath + String.valueOf(i);

				if (peerInfo.getReplicationDegs(codedPath, chunkId) == null)
					peerInfo.addFileChunk(fileName, i, (final_byte - initial_byte) / 1000);

				int j = 0;
				while (peerInfo.getReplicationDegs(codedPath, chunkId).size() < replicationDegree) {
					if (j == 5) {
						System.out.println("Couldn't achieve replication degree on chunk " + String.valueOf(i));
						break;
					}
					MDB.send(message);
					Thread.sleep((int) (Math.pow(2, j) * 1000));
					j++;
				}

				i++;
				initial_byte = final_byte;
			} while (final_byte < bytes.length);

			if (bytes.length % BLOCK_SIZE == 0) { // If file size is multiple of BLOCK_SIZE
				byte[] header = (new Header(this.version, MessageType.PUTCHUNK, this.id, codedPath, i,
						replicationDegree)).getHeader().getBytes();
				byte[] body = new byte[0];

				byte[] message = Arrays.copyOf(header, header.length + body.length);
				System.arraycopy(body, 0, message, header.length, body.length);

				System.out.println("Sending chunk nº" + String.valueOf(i));

				String chunkId = codedPath + String.valueOf(i);

				if (peerInfo.getReplicationDegs(codedPath, chunkId) == null)
					peerInfo.addFileChunk(fileName, i, (final_byte - initial_byte) / 1000);

				int j = 0;
				while (peerInfo.getReplicationDegs(codedPath, chunkId).size() < replicationDegree) {
					if (j == 5) {
						System.out.println("Couldn't achieve replication degree on chunk " + String.valueOf(i));
						break;
					}
					MDB.send(message);
					Thread.sleep((int) (Math.pow(2, j) * 1000));
					j++;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void delete(String fileName) throws RemoteException {

		try {
			FileInfo fileInfo = this.peerInfo.getFileInfo(fileName);
			if (fileInfo == null) {
				System.out.println("File doesn't exist in this peer!");
				return;
			}
			byte[] header = (new Header(this.version, MessageType.DELETE, this.id, fileInfo.getId())).getHeader()
					.getBytes();

			System.out.println("Deleting File: " + fileName);

			int i = 3;
			do {
				MC.send(header);
				Thread.sleep(1000);
				i -= 1;
			} while (i > 0);

			System.out.println("File deleted");

			this.peerInfo.removeFileInfo(fileName);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public String hash(String fileName, String path) throws Exception {
		// make a string with a mix of file_name, date_modified, owner
		Path endPath = Paths.get(path);
		BasicFileAttributes fileInfo = Files.readAttributes(endPath, BasicFileAttributes.class);
		String dateModified = fileInfo.lastModifiedTime().toString();
		String fileId = dateModified + fileName;
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] encodedhash = digest.digest(fileId.getBytes(StandardCharsets.UTF_8));
		return bytesToHex(encodedhash);

	}

	private static String bytesToHex(byte[] hash) {
		StringBuilder hexString = new StringBuilder(2 * hash.length);
		for (int i = 0; i < hash.length; i++) {
			String hex = Integer.toHexString(0xff & hash[i]);
			if (hex.length() == 1) {
				hexString.append('0');
			}
			hexString.append(hex);
		}
		return hexString.toString();
	}

	public void restore(String fileName) throws RemoteException {
		String path = testFolder + fileName;
		try {
			String addressName = "localhost";
			FileInfo fileInfo = this.peerInfo.getFileInfo(fileName);
			if (fileInfo == null) {
				System.out.println("File doesn't exist in this peer!");
				return;
			}
			File file = new File(this.peerInfo.getPeerDir() + "/" + fileInfo.getPath());
			if (file.exists())
				file.delete();
			int j = 0;
			for (int i = 0; i < fileInfo.getChunksInfo().size(); i++) {
				synchronized (MDR) {
					int port = 0;
					InetAddress address;
					ServerSocket serverSocket = null;
					if (version > 1.2) {
						port = 8000 + new Random().nextInt(1000);
						address = InetAddress.getByName(addressName);
						serverSocket = new ServerSocket(port, 1, address);
						serverSocket.setSoTimeout(1000);
					}
					while (!receivedChunk && j < 5) {
						this.lastReceivedChunkId = "";

						System.out.println("Restoring chunk nº " + i + " from file " + fileInfo.getPath());
						this.chunkToReceive = fileInfo.getId() + i;
						if (version > 1.2) {
							byte[] header = (new Header(this.version, MessageType.GETCHUNK, this.id, fileInfo.getId(), i, addressName, port))
									.getHeader().getBytes();
							this.sendTCP(serverSocket, header);
						} else {
							byte[] header = (new Header(this.version, MessageType.GETCHUNK, this.id, fileInfo.getId(), i))
							.getHeader().getBytes();
							MC.send(header);
							try {
								Thread.sleep(1000);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						j++;
					}
					if (version > 1.2)
						serverSocket.close();
					if (j >= 5) {
						System.out.println("Couldn't restore file.");
						if (file.exists())
							file.delete();
						this.receivedChunk = false;
						break;
					} else {
						j = 0;
					}
					this.receivedChunk = false;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendTCP(ServerSocket serverSocket, byte[] header) {
		byte[] message;
		MC.send(header);
		try {
			Socket socket = serverSocket.accept();
			message = receive_packet(socket);
			socket.close();
		} catch (Exception e) {
			//e.printStackTrace();
			return;
		}

		RecoveryChannelHandler.handle(this, message);
	}

    public byte[] receive_packet(Socket socket) throws IOException{
        InputStream inputStream = socket.getInputStream();

        byte[] message = inputStream.readAllBytes();

        socket.shutdownInput();

        return message;
    }

	public Integer getRemainingSpace() {
		return this.peerInfo.getCapacity() - this.peerInfo.calculateUsedStorage();
	}

	public void reclaim(int diskSpace) throws RemoteException {
		this.peerInfo.setCapacity(diskSpace);

		int usedStorage = peerInfo.calculateUsedStorage();
		if (usedStorage > diskSpace) {
			this.getSpace(usedStorage - diskSpace, true);
		}

		System.out.println("");
		System.out.println("Storage updated to " + diskSpace + " Kbytes");
		System.out.println("");
	}

	public boolean getSpace(int space, boolean force) {
		List<ChunkInfo> chunksRemoved = peerInfo.emptySpace(space, force);
		for (ChunkInfo chunk : chunksRemoved) {
			Header header = new Header(version, MessageType.REMOVED, this.id, chunk.getFileId(), chunk.getChunkNo());
			MC.send(header.getHeader().getBytes());
		}
		return chunksRemoved.size() > 0;
	}

	public void state() throws RemoteException {
		System.out.println("");
		System.out.println("-- Peer " + id + " --");
		System.out.println("");
		this.peerInfo.state("");
		System.out.println("");
		System.out.println("");
	}

	public static void main(String[] args) {
		if (args.length != 9) {
			System.out.println(
					"Usage: Peer <version> <server id> <access_point> <MC_IP_address> <MC_port> <MDB_IP_address> <MDB_port> <MDR_IP_address> <MDR_port>");
			System.exit(1);
		}

		try {

			Double version = Double.parseDouble(args[0]);
			int id = Integer.parseInt(args[1]);
			String accessPoint = args[2];

			String MCAddress = args[3];
			int MCPort = Integer.parseInt(args[4]);

			String MDBAddress = args[5];
			int MDBPort = Integer.parseInt(args[6]);

			String MDRAddress = args[7];
			int MDRPort = Integer.parseInt(args[8]);

			Peer peer = new Peer(version, id, accessPoint, MCAddress, MCPort, MDBAddress, MDBPort, MDRAddress, MDRPort);

			RMIStub stub = (RMIStub) UnicastRemoteObject.exportObject(peer, 0);

			// Bind the remote object's stub in the registry
			Registry registry = LocateRegistry.getRegistry();
			registry.bind(accessPoint, stub);

			peer.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
