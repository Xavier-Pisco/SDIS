import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class Client {
    public static void main(String[] args) throws IOException {
        DatagramSocket socket = new DatagramSocket();
        InetAddress address = InetAddress.getByName(args[0]);
        String message = new String();
        if (args[2].equals("register")) {
            message = "register " + args[3] + " " + args[4];
        } else if (args[2].equals("lookup")) {
            message = "lookup " + args[3];
        } else {
            System.out.println("Usage: java Client <host> <port> <oper> <opnd>*");
            System.out.println("<oper> = \"register\" or \"lookup\"");
            socket.close();
            return;
        }
        byte[] buffer = message.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, Integer.parseInt(args[1]));
        socket.send(packet);
        buffer = new byte[256];
        packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        String received = new String(packet.getData(), 0, packet.getLength());
        System.out.println("Client: " + args[2] + " " + args[3] + " " + (args[2].equals("register") ? args[4] + " : " : " : ") + received);
        socket.close();
    }
}
