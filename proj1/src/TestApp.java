import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.io.IOException;
import java.net.ConnectException;

public class TestApp {
    RMIStub stub;

    public TestApp(RMIStub stub) {
        this.stub = stub;
    }

    public static void main(String args[]) throws Exception{
		String accessPoint = args[0];
		TestApp app;
		try {
			Registry registry = LocateRegistry.getRegistry();
			RMIStub stub = (RMIStub) registry.lookup(args[0]);

			app = new TestApp(stub);
		} catch (Exception e) {
			System.out.println("Could not connect to peer on access point " + accessPoint);
			return;
		}

		switch(args[1]) {
			case "BACKUP":
				app.backup(args[2], Integer.parseInt(args[3]));
				break;
			case "RESTORE":
				app.restore(args[2]);
				break;
			case "DELETE":
				app.delete(args[2]);
				break;
			case "RECLAIM":
				app.reclaim(Integer.parseInt(args[2]));
				break;
			case "STATE":
				app.state();
				break;
			default:
				System.out.println("Unrecognized operation");
				System.exit(1);
		}
		//multicastSocket.close();
    }

	public void backup(String path, int replicationDegree) throws Exception {
        this.stub.backup(path, replicationDegree);
	}
    public void restore(String path) throws Exception {
        this.stub.restore(path);
	}
    public void delete(String path) throws Exception {
        this.stub.delete(path);
	}
    public void reclaim(int diskSpace) throws Exception {
        this.stub.reclaim(diskSpace);
	}
    public void state() throws Exception {
        this.stub.state();
	}
}
