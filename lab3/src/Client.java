import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

    private Client() {}

    public static void main(String[] args) {

        try {
            Registry registry = LocateRegistry.getRegistry(args[0]);
            DNSTable stub = (DNSTable) registry.lookup(args[1]);
            String[] arguments;
            if (args.length == 4) {
                arguments = new String[2];
            } else {
                arguments = new String[3];
            }
            for (int i = 2; i < args.length; i++) {
                arguments[i - 2] = args[i];
            }
            String response = stub.dns_entries(arguments);
            System.out.println("response: " + response);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}

//