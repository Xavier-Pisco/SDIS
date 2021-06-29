import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.net.*;

public class Server {
    private static Map<String, InetAddress> DNSTable = new HashMap<>();
    public static void main(String[] args) throws IOException {
        int portNumber = Integer.parseInt(args[0]);
        DatagramSocket socket = new DatagramSocket(portNumber);
        byte[] buffer = null;
        DatagramPacket packet = null;
        while(true) {
            buffer = new byte[256];
            packet = new DatagramPacket(buffer, buffer.length);
            String response = receive_packet(socket, packet);
            send_packet(socket, packet, response);
        }
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
        System.out.println("Server: " + received);
        String response = dns_entries(received.split(" "));
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

}
