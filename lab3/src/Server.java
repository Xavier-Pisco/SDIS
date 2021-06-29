import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.net.*;
import java.rmi.registry.*;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;



public class Server implements DNSTable {
    private static Map<String, InetAddress> DNSTable = new HashMap<>();
    public Server() {}

    public static void main(String args[]) {

        try {
            Server obj = new Server();
            DNSTable stub = (DNSTable) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind(args[0], stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
    public String dns_entries(String[] args) throws RemoteException{
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
}
