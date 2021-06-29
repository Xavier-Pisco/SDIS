import java.rmi.Remote;
import java.rmi.RemoteException;
import java.io.IOException;
import java.net.*;


public interface DNSTable extends Remote {
    String dns_entries(String[] args) throws RemoteException;
}