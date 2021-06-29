import java.net.UnknownHostException;
import java.net.DatagramPacket;

public class RecoveryChannel extends MulticastChannel {

	public RecoveryChannel(String address, int port, Peer peer) throws UnknownHostException {
		super(address, port, peer);
	}

	public void handle(DatagramPacket packet) {
		this.peer.getExecutor().execute(new RecoveryChannelHandler(peer, packet));
	}
}
