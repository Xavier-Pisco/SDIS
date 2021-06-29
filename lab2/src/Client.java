import java.io.IOException;
import java.net.*;

public class Client {
	public static void main(String args[]) throws IOException{
		int mcast_port = Integer.parseInt(args[1]);

        String message = new String();
        if (args[2].equals("register")) {
            message = "register " + args[3] + " " + args[4];
        } else if (args[2].equals("lookup")) {
            message = "lookup " + args[3];
        } else {
            System.out.println("Usage: java Client <mcast_addr> <mcast_port> <oper> <opnd>*");
            System.out.println("<oper> = \"register\" or \"lookup\"");
            return;
        }

		MulticastSocket multicastSocket = new MulticastSocket(mcast_port);
		InetAddress group = InetAddress.getByName(args[0]);
        multicastSocket.joinGroup(group);
		byte[] buffer = new byte[256];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        multicastSocket.receive(packet);
        String[] received = new String(packet.getData(), 0, packet.getLength()).split(" ");
		multicastSocket.close();

        InetAddress srvc_addr = InetAddress.getByName(received[0]);
        int srvc_port = Integer.parseInt(received[1]);

        DatagramSocket datagramSocket = new DatagramSocket();
        buffer = message.getBytes();
        packet = new DatagramPacket(buffer, buffer.length, srvc_addr, srvc_port);
        datagramSocket.send(packet);
        System.out.println("multicast: " + args[0] + " " + String.valueOf(mcast_port) +
                            ": " + received[0] + " " + String.valueOf(srvc_port));


        buffer = new byte[256];
        packet = new DatagramPacket(buffer, buffer.length);
        datagramSocket.receive(packet);
        String serverResponse = new String(packet.getData(), 0, packet.getLength());
        System.out.println(message + " :: " + serverResponse);

        datagramSocket.close();
	}
}
