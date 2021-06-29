import java.net.UnknownHostException;
import java.net.DatagramPacket;

public class ControlChannel extends MulticastChannel {
	public ControlChannel(String address, int port, Peer peer) throws UnknownHostException {
		super(address, port, peer);
	}

	public void handle(DatagramPacket packet) {
		this.peer.getExecutor().execute(new ControlChannelHandler(peer, packet));
	}
}
