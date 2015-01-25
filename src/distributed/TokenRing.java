package distributed;

import java.net.MalformedURLException;

import org.apache.xmlrpc.XmlRpcException;

public class TokenRing extends SyncAlgorithm {
	private boolean token = false;
	public boolean hasAccess() {
		return token;
	}
	public void requestAccess() {
		if(!checkState("wanted")) {
			setState("wanted");
		}
	}
	public void getToken() {
		token = true;
	}
	
	private String nextInRing = "";
	public void setRing(String node) {
		nextInRing = node;
		doneNodes.clear();
		token = false;
	}
	
	public void initializeToken() {
		getToken();
		//freeResource();
	}
	
	public TokenRing() { }
	
	synchronized public void freeResource() {
		if(!token)
			return;
		
		if((doneNodes.size() < netLength) && 
				!(doneNodes.size()==netLength-1 && !doneNodes.contains(ServerSide.getOwnHostAddress()))) {
			token = false;
			ClientSide ownClient;
			try {
				ownClient = new ClientSide(ServerSide.getOwnHostAddress());
				ownClient.sender.execute("PDSProject.passToken", new Object[]{nextInRing});
			} catch (MalformedURLException e1) {
				System.out.println("Could not make internal server call");
				System.out.println(e1.getMessage());
			}
			catch (XmlRpcException e) {
				System.out.println("Could not make internal server call");
				System.out.println(e.getMessage());
			}
		}
	}
}
