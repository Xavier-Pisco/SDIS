import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.net.*;

public class Server {
    private static Map<String, InetAddress> DNSTable = new HashMap<>();
    public static void main(String[] args) throws IOException {
        int portNumber = Integer.parseInt(args[0]);
        InetAddress address = InetAddress.getByName(args[1]);
        ServerSocket serverSocket = new ServerSocket(portNumber, 1, address);
        while(true) {
            Socket socket = serverSocket.accept();
            String response = receive_packet(socket);
            System.out.println("response: " + response);
            send_packet(socket, response);
            //serverSocket.close();
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
    public static String receive_packet(Socket socket) throws IOException{
        InputStream inputStream = socket.getInputStream();

        byte[] message = inputStream.readAllBytes();

        String received = new String(message);
        socket.shutdownInput();

        System.out.println("Server: " + received);
        String response = dns_entries(received.split(" "));
        return response;
    }

    public static void send_packet(Socket socket, String response) throws IOException{
        OutputStream outputStream = socket.getOutputStream();

        byte[] byteResponse = response.getBytes();

        outputStream.write(byteResponse, 0, byteResponse.length);

        socket.shutdownOutput();
    }

}
