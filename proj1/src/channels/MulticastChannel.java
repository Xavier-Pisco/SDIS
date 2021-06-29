import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ConcurrentModificationException;
import java.net.*;

/**
 * Class for all the multicast channels that are going to be used In the peer
 * for the MC, MDB, MDR
 */
public abstract class MulticastChannel implements Runnable {
	private InetAddress address;
	private int port;
	private String addressString;
	Peer peer;

	public MulticastChannel(String address, int port, Peer peer) throws UnknownHostException {
		this.addressString = address;
		this.port = port;
		this.address = InetAddress.getByName(address);
		this.peer = peer;
	}

	public void run() {
		try {
			MulticastSocket receiveSocket = new MulticastSocket(port);
			receiveSocket.joinGroup(address);

			while (true) {
				byte[] buf = new byte[65000];
				DatagramPacket receivedPacket = new DatagramPacket(buf, buf.length);

				receiveSocket.receive(receivedPacket);

				this.handle(receivedPacket);
				this.peer.saveInfo();
			}
		} catch (ConcurrentModificationException cme) {
			System.out.println("Small error on peer.saveInfo");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public abstract void handle(DatagramPacket receivedPacket);

	public void send(byte[] buffer) {
		try {
			MulticastSocket sendSocket = new MulticastSocket(port);
			sendSocket.joinGroup(address);

			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, this.address, this.port);
			sendSocket.send(packet);
			sendSocket.leaveGroup(address);
		} catch (SocketException e) {
			//e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
