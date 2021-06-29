import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.*;

import javax.net.ssl.*;

import java.net.*;

public class Server {
    private static Map<String, InetAddress> DNSTable = new HashMap<>();
    public static void main(String[] args) throws IOException {
        int portNumber = Integer.parseInt(args[0]);
        String[] cypher = new String[args.length - 1];
        for (int i = 1; i < args.length; i++) {
            cypher[i - 1] = args[i];
        }

        SSLServerSocket serverSocket = (SSLServerSocket) SSLServerSocketFactory.getDefault().createServerSocket(portNumber);
        serverSocket.setEnabledCipherSuites(cypher);

        while(true) {
            SSLSocket socket = (SSLSocket)serverSocket.accept();
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();
            System.out.println("Server receiving");
            String response = receive_packet(inputStream);
            System.out.println("Server writing");
            System.out.println("response: " + response);
            send_packet(outputStream, response);
            System.out.println("Server wrote");
            outputStream.flush();
            inputStream.close();
            outputStream.close();
            socket.close();
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
    public static String receive_packet(InputStream inputStream) throws IOException{

        byte[] message = inputStream.readAllBytes();
        String received = new String(message);

        System.out.println("Server: " + received);
        String response = dns_entries(received.split(" "));
        return response;
    }

    public static void send_packet(OutputStream outputStream, String response) throws IOException{
        byte[] byteResponse = response.getBytes();

        outputStream.write(byteResponse, 0, byteResponse.length);

    }

}
