import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.util.*;

import javax.net.ssl.*;

public class Client {
    public static void main(String[] args) throws IOException {
        int portNumber = Integer.parseInt(args[1]);
        InetAddress address = InetAddress.getByName(args[0]);
        String[] cypher = null;

        String message = new String();
        if (args[2].equals("register")) {
            message = "register " + args[3] + " " + args[4];
            cypher = new String[args.length - 5];
            for (int i = 5; i < args.length; i++) {
                cypher[i - 5] = args[i];
            }
        } else if (args[2].equals("lookup")) {
            message = "lookup " + args[3];
            cypher = new String[args.length - 4];
            for (int i = 4; i < args.length; i++) {
                cypher[i - 4] = args[i];
            }
        } else {
            System.out.println("Usage: java Client <host> <port> <oper> <opnd>*");
            System.out.println("<oper> = \"register\" or \"lookup\"");
            return;
        }

        SSLSocket socket = (SSLSocket)SSLSocketFactory.getDefault().createSocket(address, portNumber);
        socket.setEnabledCipherSuites(cypher);

        //socket.startHandshake();

        byte[] buffer = message.getBytes(StandardCharsets.UTF_8);

        OutputStream outputStream = socket.getOutputStream();
        InputStream inputStream = socket.getInputStream();

        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);

        System.out.println("Client writing");
        //outputStream.write(buffer, 0, buffer.length);
        //outputStream.flush();
        bufferedOutputStream.write(buffer, 0, buffer.length);
        bufferedOutputStream.flush();
        System.out.println("Client receiving");
        
        
        buffer = inputStream.readAllBytes();
        System.out.println("Client received");
        
        String received = new String(buffer);
        System.out.println("Client: " + args[2] + " " + args[3] + " " + (args[2].equals("register") ? args[4] + " : " : " : ") + received);
        outputStream.close();
        inputStream.close();
        socket.close();
    }
}
