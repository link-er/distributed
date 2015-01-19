package distributed;

import java.net.MalformedURLException;

import org.apache.xmlrpc.XmlRpcException;

public class TokenRing extends SyncAlgorithm {
	private boolean token = false;
	public boolean hasAccess() {
		return token;
	}
	public void getToken() {
		token = true;
	}
	
	private String nextInRing = "";
	public void setRing(String node) {
		nextInRing = node;
	}
	
	public TokenRing() { }
	
	public void freeResource() {
		token = false;
		if(doneNodes.size() < netLength) {
			ClientSide client = null;
			try {
				client = new ClientSide(nextInRing);
			} catch (MalformedURLException e1) {
				System.out.println("Failed to create client to next in ring");
			}
			try {
				System.out.println(Helper.logStart(0) + "send token to " + nextInRing);
				client.sender.execute("PDSProject.receiveToken", new Object[]{});
			} catch (XmlRpcException e) {
				System.out.println("Failed to send token");
			}
		}
	}
}
