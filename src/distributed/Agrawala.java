package distributed;

import java.net.*;
import java.util.*;

import org.apache.xmlrpc.XmlRpcException;

public class Agrawala extends SyncAlgorithm {
	public Agrawala() { }
	
	public void setState(String value) {
		super.setState(value);
		if(value.equals("wanted")) {
			requestAccess();
		}
	}
	
	private List<String> queue = new ArrayList<String>();
	
	public void freeResource() {
		System.out.println(Helper.logStart(getTimestamp()) + "free resource, send OK to " +
				Arrays.toString(queue.toArray()));
		for(String node : queue) {
			URL nodeUrl;
			try {
				nodeUrl = new URL(node);
				ClientSide client = new ClientSide(nodeUrl);
				System.out.println(Helper.logStart(getTimestamp()) + "send event");
				incrementTimestamp();
				client.sender.execute("PDSProject.receiveOk", new Object[]{timestamp});
			} catch (MalformedURLException e) {
				System.out.println("Bad value in queue of requesters");
				System.out.println(e.getMessage());
			} catch (XmlRpcException e) {
				System.out.println("Could not send OK");
				System.out.println(e.getMessage());
			}
		}
		queue.clear();
	}
	
	public void enque(String nodeId, int requesterTimestamp) {
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
	
	public boolean hasAccess() {
		return okFromNodes == netLength - 1;
	}
	
	public void requestAccess() {
		boolean answer;
		okFromNodes = 0;
		for(URL netHost : ServerSide.getNetBroadcast()) {
			ClientSide client = new ClientSide(netHost);
			try {
				incrementTimestamp();
				answer = (Boolean) client.sender.execute("PDSProject.receiveRequest", 
						new Object[]{ServerSide.getOwnHostAddress(), timestamp});
				System.out.println(Helper.logStart(getTimestamp()) + "got answer on request " + answer +
						" from " + netHost.toString());
				if(answer)
					okFromNodes++;
			} catch (XmlRpcException e) {
				System.out.println("Failed to request access from hosts.");
			}
		}
	}
}
