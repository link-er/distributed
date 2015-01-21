package distributed;

import java.io.IOException;
import java.net.*;
import java.util.*;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.*;
import org.apache.xmlrpc.webserver.WebServer;

public class ServerSide {
	private final int port = 8080;
	private WebServer webServer;
	
//  indexed structure of calculative net
	private static Set<String> net = new HashSet<String>();

//	method for getting simple String array of hosts in net
//	used for return value for join request
	public static String[] getNetSimpleArray() {
		String[] result = new String[net.size()];
		int index = 0;
		for(String host : net) {
			result[index] = host;
			index++;
		}
		return result;
	}
	
//	initialize lists with own address - in case if we are the first asked for join
	public static void ownHostFirstInitialize() {
		if(net.size()==0)
			net.add(ownHostAddress);
	}

//	method for broadcasting messages
//	do not include ourself in the broadcast list
	public static URL[] getNetBroadcast() {
		URL[] result = new URL[net.size() - 1];
		int i = 0;
		for(String host : getNetSimpleArray()) {
			if(!host.equals(ownHostAddress)) {
				try {
					result[i] = new URL(host);
				} catch (MalformedURLException e) {
					System.out.println(e.getMessage());
				}
				i++;
			}
		}
		return result;
	}
	
//	saved in full form, with http
	private static String ownHostAddress;
	public static String getOwnHostAddress() {
		return ownHostAddress;
	}

//	we are in net if net array is not empty
	public boolean inNet() {
		return net.size() > 0;
	}

//	net length is all saved nodes address and the node itself
	public static int netLength() {
		return net.size();
	}

	public ServerSide(String typedInIp) throws IOException, XmlRpcException {
		webServer = new WebServer(port);
		XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();
		PropertyHandlerMapping phm = new PropertyHandlerMapping();
		phm.load(Thread.currentThread().getContextClassLoader(), "XmlRpcServlet.properties");
		xmlRpcServer.setHandlerMapping(phm);
		XmlRpcServerConfigImpl serverConfig = (XmlRpcServerConfigImpl) xmlRpcServer.getConfig();
		serverConfig.setEnabledForExtensions(true);
		serverConfig.setContentLengthOptional(false);
		
		ownHostAddress = Helper.formUrlString(typedInIp + ":" + port);
	}
	
	public void start() throws IOException {
		webServer.start();
	}

	public void stop() {
		webServer.shutdown();
	}

//	action to execute when we received answer to join call
	public void joinedToNet(Object existingNet) {
		if(existingNet != null) {
			String existingNetString = (String) existingNet;
			for(String host : existingNetString.split(",")) {
//				expect host to be in form http://address:port/pds/
				net.add((String) host);
			}
			
		}
	}

//	action to be called when node is joining to net
	public static void addHost(String joiningAddress) {
		net.add(joiningAddress);
	}

//	action to execute when we received answer to signOff call from everybody
	public void signedOffNet() {
		net.clear();
	}

//	action to be called when node is signing off the net
	public static void removeHost(String signingOffAddress) {
		net.remove(signingOffAddress);
	}
	
//	=========SYNC=======
	private static SyncAlgorithm algorithm;
	
	public static void setAlgorithm() {
		algorithm.setNetLength(netLength());
		algorithm.setRing(getNextInRing());
	}
	
	public static void setAlgorithm(String algorithmName) {
		if(algorithmName.equals("tr")) {
			algorithm = new TokenRing();
		}
		else {
			algorithm = new Agrawala();
		}
	}
	
	public static void updateTimestamp(int requesterTimestamp) {
		algorithm.updateTimestamp(requesterTimestamp);
	}
	
	public static int getTimestamp() {
		return algorithm.getTimestamp();
	}
	
	public static void startTimestamp() {
		algorithm.startTimestamp();
	}
	
	public static boolean hasAccess() {
		return algorithm.hasAccess();
	}
	
	public static void setState(String value) {
		algorithm.setState(value);
	}
	
	public static boolean checkState(String value) {
		return algorithm.checkState(value);
	}
	
	public static void setDone() {
		algorithm.setDone();
	}
	
	public static void addDoneHost(String host) {
		algorithm.addDoneHost(host);
	}
	
	public static void requestAccess() {
		algorithm.requestAccess();
	}
	
	public static class Operation implements Comparable<Operation> {
		public int compareTo(Operation other){ 
			return 0;
		}
		private int operation;
		public int getOperation() {
			return operation;
		}
		private int operand;
		public int getOperand() {
			return operand;
		}
		public Operation(int operation, int operand) {
			this.operand = operand;
			this.operation = operation;
		}
		
		public int implement(int value) {
			switch(operation) {
				case(0): {
					return value += operand;
				}
				case(1): {
					return value -= operand;
				}
				case(2): {
					return value *= operand;
				}
				case(3): {
					return value /= operand;
				}
				default: {
					return value;
				}
			}
		}
	}
	
	private static PriorityQueue<Operation> operationsQueue = new PriorityQueue<Operation>();
	
	public static void queueOperation(int operation, int operand) {
		operationsQueue.add(new Operation(operation, operand));
	}
	
//	dummy method for token ring, because we will never have access - token is always false
//	for token ring queue will be finished by receiving token method
	public static void finishQueue(int value) {
		int result = value;
		while(!operationsQueue.isEmpty()) {
			ServerSide.requestAccess();
			if(hasAccess())
				calculate(result);
			result = DistributedServer.getValue();
		}
	}
	
	public static void calculate(int value) {
		setState("locked");
		System.out.println(Helper.logStart(getTimestamp()) + "entering critical section!");
		Operation current = operationsQueue.poll();
		if(current!=null) {
			algorithm.incrementTimestamp();
			int result = current.implement(value);
			DistributedServer.setNewValue(result);
			propagateCalculation(current.getOperation(), current.getOperand());
			System.out.println(Helper.logStart(getTimestamp()) + "on " + value + " made operation " + current.getOperation() + " with "
					+ current.getOperand() + " and got ***" + result + "***");
		}
		setState("free");
		algorithm.freeResource();
		System.out.println(Helper.logStart(getTimestamp()) + "finished critical section");
	}
	
	public static void propagateCalculation(int operationToMake, int operand) {
		for(URL netHost : getNetBroadcast()) {
			ClientSide client = new ClientSide(netHost);
			try {
				algorithm.incrementTimestamp();
				System.out.println(Helper.logStart(getTimestamp()) + "send event");
				client.sender.execute("PDSProject.performOperation", new Object[]{operationToMake, operand, getTimestamp()});
			} catch (XmlRpcException e) {
				System.out.println("Failed to propagate our calculation to hosts.");
			}
		}
	}
	
//	=======TOKEN RING======
	public static String getNextInRing() {
		int ownIndex = -1;
		String[] sortedNet = getNetSimpleArray();
		Arrays.sort(sortedNet);
		int currentIndex = 0;
		for(String host : sortedNet) {
			if(host.equals(ownHostAddress))
				ownIndex = currentIndex;
			currentIndex++;
		}
		if(ownIndex==netLength()-1)
			return sortedNet[0];
		else
			return sortedNet[ownIndex + 1];
	}
	
	public static void freeResource() {
		algorithm.freeResource();
	}
	
	public static void getToken() {
		TokenRing tr = (TokenRing) algorithm;
		tr.getToken();
	}
	
//	=========AGRAWALA========
	public static void enque(String nodeId) {
		Agrawala agr = (Agrawala) algorithm;
		agr.enque(nodeId);
	}
	
	public static void addAccessAnswer() {
		Agrawala agr = (Agrawala) algorithm;
		agr.addAccessAnswer();
	}
	
	public static int getWantedTimestamp() {
		Agrawala agr = (Agrawala) algorithm;
		return agr.getWantedTimestamp();
	}
}
