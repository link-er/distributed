package distributed;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import org.apache.xmlrpc.XmlRpcException;

public class SyncAlgorithm {
	protected String state = "free";
	public void setState(String value) {
		state = value;
	}
	public boolean checkState(String value) {
		return state.equals(value);
	}
	
	protected int netLength;
	public void setNetLength(int length) {
		netLength = length;
	}
	
	public boolean hasAccess() {
		System.out.println("NOT IMPLEMENTED!");
		return false;
	}
	
	public void freeResource() {
		System.out.println("NOT IMPLEMENTED!");
	}
	
//	only for Agrawala, but called in both algorithms
	public void incrementTimestamp() { }
	public void updateTimestamp(int requesterTimestamp) { }
	public int getTimestamp() { return 0; }
	public void startTimestamp() { }
	public void requestAccess() { }
	
//	only for token ring, but called in both algorithms
	public void setRing(String node) { }
	public void initializeToken() { }
	protected Set<String> doneNodes = new HashSet<String>();
	public void setDone() {
		doneNodes.add(ServerSide.getOwnHostAddress());
		for(URL netHost : ServerSide.getNetBroadcast()) {
			ClientSide client = new ClientSide(netHost);
			try {
				client.sender.execute("PDSProject.done", new Object[]{ServerSide.getOwnHostAddress()});
			} catch (XmlRpcException e) {
				System.out.println("Failed to propagate that we are done to hosts.");
			}
		}
	}
	public void addDoneHost(String doneHost) {
		doneNodes.add(doneHost);
	}
}
