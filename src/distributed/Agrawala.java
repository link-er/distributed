package distributed;

import java.net.*;
import java.util.*;
import org.apache.xmlrpc.XmlRpcException;

public class Agrawala extends SyncAlgorithm {
	public Agrawala() { }
	
	private List<String> queue = new ArrayList<String>();
	public String[] getQueue() {
		return queue.toArray(new String[0]);
	}
	
	public void freeResource() {
		System.out.println(Helper.logStart(getTimestamp()) + "free resource, send OK to " +
				Arrays.toString(queue.toArray()));
		ClientSide ownClient;
		try {
			ownClient = new ClientSide(ServerSide.getOwnHostAddress());
			ownClient.sender.execute("PDSProject.sendOks", new Object[]{});
		} catch (MalformedURLException e1) {
			System.out.println("Could not make internal server call");
			System.out.println(e1.getMessage());
		}
		catch (XmlRpcException e) {
			System.out.println("Could not make internal server call");
			System.out.println(e.getMessage());
		}
		queue.clear();
	}
	
	public void enque(String nodeId) {
		System.out.println(Helper.logStart(getTimestamp()) + "adding " + nodeId + " to queue");
		queue.add(nodeId);
	}
	
	private int timestamp = 0;
	public int getTimestamp() {
		return timestamp;
	}
	public void startTimestamp() { 
		timestamp = 0;
	}
	public void incrementTimestamp() {
		timestamp++;
	}
	public void updateTimestamp(int requesterTimestamp) {
		if(requesterTimestamp > timestamp)
			timestamp = requesterTimestamp + 1;
		else
			incrementTimestamp();
	}
	
	private int okFromNodes = 0;
	
	public void addAccessAnswer() {
		okFromNodes++;
	}
	
	private int wantedTimestamp = 0;
	public int getWantedTimestamp() {
		return wantedTimestamp;
	}
	
	public boolean hasAccess() {
		return okFromNodes == netLength - 1;
	}
	
	public void requestAccess() {
		if(checkState("wanted"))
			return;
		
		setState("wanted");
		okFromNodes = 0;
		wantedTimestamp = timestamp;
		ClientSide ownClient;
		try {
			ownClient = new ClientSide(ServerSide.getOwnHostAddress());
			ownClient.sender.execute("PDSProject.sendRequests", new Object[]{wantedTimestamp});
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
