package distributed;

import java.net.MalformedURLException;

import org.apache.xmlrpc.XmlRpcException;

public class TokenRing extends SyncAlgorithm {
	private boolean token = false;
	public boolean hasAccess() {
		return token;
	}
	
	private String nextInRing = "";
	public void setRing(String node) {
		nextInRing = node;
	}
	
	public TokenRing() {
//	    give token to our node
//		only before first sending token to the next in ring, after that it will always be false
		token = true;
	}
	
	public void freeResource() {
		if(doneNodes.size() < netLength) {
			ClientSide client = null;
			try {
				client = new ClientSide(nextInRing);
			} catch (MalformedURLException e1) {
				System.out.println("Failed to create client to next in ring");
			}
			try {
				System.out.println("------Server talking: send token to " + nextInRing);
				client.sender.execute("PDSProject.receiveToken", new Object[]{});
				token = false;
			} catch (XmlRpcException e) {
				System.out.println("Failed to send token");
			}
		}
	}
}
