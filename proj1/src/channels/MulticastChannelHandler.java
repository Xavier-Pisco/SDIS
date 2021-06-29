import java.net.DatagramPacket;

public abstract class MulticastChannelHandler implements Runnable {
	protected Peer peer;
	protected DatagramPacket receivedPacket;

	public MulticastChannelHandler(Peer peer, DatagramPacket packet) {
		this.peer = peer;
		this.receivedPacket = packet;
	}

	public abstract void run();
}
