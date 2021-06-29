import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class Client {
    public static void main(String[] args) throws IOException {
        int portNumber = Integer.parseInt(args[0]);
        InetAddress address = InetAddress.getByName(args[1]);
        Socket socket = new Socket(address, portNumber);

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

        OutputStream outputStream = socket.getOutputStream();

        outputStream.write(buffer, 0, buffer.length);

        socket.shutdownOutput();

        InputStream inputStream = socket.getInputStream();

        buffer = inputStream.readAllBytes();

        String received = new String(buffer);
        socket.shutdownInput();
        System.out.println("Client: " + args[2] + " " + args[3] + " " + (args[2].equals("register") ? args[4] + " : " : " : ") + received);
    }
}
