import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.net.*;

public class Server {
    private static Map<String, InetAddress> DNSTable = new HashMap<>();
    public static void main(String args[]) throws IOException {
        int srvc_port = Integer.parseInt(args[0]);
        int mcast_port = Integer.parseInt(args[2]);
		MulticastSocket socket = new MulticastSocket(mcast_port);
        DatagramSocket service = new DatagramSocket(srvc_port);
        InetAddress group = InetAddress.getByName(args[1]);
        socket.joinGroup(group);
        service.setSoTimeout(1000);

        while(true) {
            byte[] buffer = new byte[256];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            String response = null;
            try {
                response = receive_packet(service, packet);
                send_packet(service, packet, response);
            } catch (SocketTimeoutException ste) {
                String message = "localhost" + " " + String.valueOf(service.getLocalPort());
                send_multicast_packet(socket, group, mcast_port, message);
                System.out.println("multicast: " + args[1] + " " + String.valueOf(mcast_port) +
                                    ": localhost" + String.valueOf(srvc_port));
            }
            if (response == "Quit")
                break;
        }
        socket.leaveGroup(group);
        socket.close();
    }
    public static String dns_entries(String[] args) throws UnknownHostException {
        if (args[0].equals("register")) {
            InetAddress ip = null;
            try {
                ip = DNSTable.putIfAbsent(args[1], InetAddress.getByName(args[2]));
            } catch (IOException e) {
                return "ERROR";
            }
            if (ip != null) {
                return "EXISTS";
            } else {
                return String.valueOf(DNSTable.size());
            }
        } else if (args[0].equals("lookup")) {
            try {
                InetAddress ip = DNSTable.get(args[1]);
                if (ip == null) {
                    return "NOT_FOUND";
                }
                return ip.getHostAddress();
            } catch(Exception e){
                return "NOT_FOUND";
            }
        } else {
            return "ERROR";
        }
    }
    public static String receive_packet(DatagramSocket socket, DatagramPacket packet) throws IOException{
        socket.receive(packet);
        String received = new String(packet.getData(), 0, packet.getLength());
        String response = dns_entries(received.split(" "));
        System.out.println(received + " :: " + response);
        return response;
    }
    public static void send_packet(DatagramSocket socket, DatagramPacket packet, String response) throws IOException{
        byte[] sendBuffer = null;
        DatagramPacket sendPacket = null;
        InetAddress address = packet.getAddress();
        int port = packet.getPort();
        sendBuffer = response.getBytes(StandardCharsets.UTF_8);
        sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, port);
        socket.send(sendPacket);
    }
    public static void send_multicast_packet(MulticastSocket socket, InetAddress group, int port, String message) throws IOException {
        byte[] sendBuffer = message.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(sendBuffer, sendBuffer.length, group, port);
        socket.send(packet);
    }
}
