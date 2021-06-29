import java.net.UnknownHostException;
import java.net.DatagramPacket;

public class BackupChannel extends MulticastChannel {

	public BackupChannel(String address, int port, Peer peer) throws UnknownHostException {
		super(address, port, peer);
	}

	public void handle(DatagramPacket packet) {
		this.peer.getExecutor().execute(new BackupChannelHandler(peer, packet));
	}
}
