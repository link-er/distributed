package distributed;

import java.util.*;

public class DistributedServer {
	private static int value;
	public static void setNewValue(int newValue) {
		value = newValue;
	}
	
//	waiting for url in form http://server:port/pds/
	public String addNode(String joiningAddress) {
		System.out.println("------Server talking: " + joiningAddress + " wants to join us!");
//		adding ourself only if it the first call for join and we have nothing in net
		ServerSide.ownHostFirstInitialize();
//		calling internal node method to add the joining host to net and remember it
		ServerSide.addHost(joiningAddress);
		System.out.println("------Server talking: " + joiningAddress + " joined successfully!");
		System.out.println("------Server talking: Net now is " + Arrays.toString(ServerSide.getNetSimpleArray()));
//		return _ordered_ array of strings - addresses of hosts in net
//		address is in form http://address:port/pds/
		String result = "";
		for(String node : ServerSide.getNetSimpleArray()) {
			result = result + "," + node;
		}
		return result.substring(1, result.length());
	}

//	waiting for url in form http://server:port/pds/
	public boolean disconnectNode(String offAddress) {
		System.out.println("------Server talking: " + offAddress + " wants to sign off.");
//		calling internal node method to remove the host from net
		ServerSide.removeHost(offAddress);
		System.out.println("------Server talking: " + offAddress + " successfully signed off!");
		System.out.println("------Server talking: Net now is " + Arrays.toString(ServerSide.getNetSimpleArray()));
		return true;
	}
	
	Thread thread = new Thread(){
		public void run(){
			ServerSide.startTimestamp();
			Random rand = new Random();
			long startTime = System.currentTimeMillis();
			while(System.currentTimeMillis() - startTime <= 20000) {
				int timeToSleep, operationToMake, operand;
				timeToSleep = rand.nextInt(10);
				try {
					System.out.println("------Server talking: sleep for " + timeToSleep + "s.");
					Thread.sleep(timeToSleep*1000);
				} catch (InterruptedException e) {
					System.out.println("Cannot sleep");
				}
				ServerSide.setState("wanted");
				operationToMake = rand.nextInt(4);
//				in order not to check if we got 0 for division
				operand = rand.nextInt(5) + 1;
				ServerSide.queueOperation(operationToMake, operand);
//				this part is not actually used for tokenRing
				if(ServerSide.hasAccess())
					ServerSide.calculate(value);
			}
			ServerSide.finishQueue(value);
			System.out.println("------Server talking: finished, got ***" + value);
			ServerSide.setDone();
		}
	};
	
	public boolean calculateValue(int initialValue) {
		value = initialValue;
		ServerSide.setAlgorithm();
		System.out.println("------Server talking: starting with initial value " + value);
		thread.start();
		return true;
	}
	
	public boolean performOperation(int operation, int operand, int requesterTimestamp) {
		ServerSide.updateTimestamp(requesterTimestamp);
		System.out.println("TIC-TAC (receive operation " + requesterTimestamp + ")" + ServerSide.getTimestamp());
		int startValue = value;
		value = new ServerSide.Operation(operation, operand).implement(value);
		System.out.println("performOperation------Server talking: called for making on " + startValue + " operation " +
				operation + " with " + operand + " and got result ***" + value);
		return true;
	}
	
	public boolean done(String doneHost) {
		System.out.println("------Server talking: " + doneHost + " finished calculations.");
//		calling internal node method to add the host to done
		ServerSide.addDoneHost(doneHost);
		return true;
	}
	
//	===========TOKEN RING
	public boolean receiveToken() {
		System.out.println("receiveToken------Server talking: got token");
		if(ServerSide.checkState("wanted"))
			ServerSide.calculate(value);
		else {
			while(ServerSide.checkState("locked"));
			ServerSide.freeResource();
		}
		return true;
	}
	
//=============ARGAWALA
	public boolean receiveRequest(String nodeId, int requesterTimestamp) {
		System.out.println("receiveRequest------Server talking: got timestamp " + requesterTimestamp
				+ ", self " + ServerSide.getTimestamp());
		boolean result;
		if(ServerSide.checkState("free")) {
			System.out.println("receiveRequest------Server talking: state is free, return OK access to " + nodeId);
			result = true;
		}
		else {
			int currentTimestamp = ServerSide.getTimestamp();
			if((ServerSide.checkState("wanted") && requesterTimestamp > currentTimestamp) ||
					(ServerSide.checkState("locked"))) {
				ServerSide.enque(nodeId, requesterTimestamp);
				System.out.println("receiveRequest------Server talking: state is not free, return no access to " + nodeId);
				result = false;
			}
			else {
				System.out.println("receiveRequest------Server talking: state is not free, but return access OK to " + nodeId);
				result = true;
			}
		}
		ServerSide.updateTimestamp(requesterTimestamp);
		System.out.println("TIC-TAC (receive operation)" + ServerSide.getTimestamp());
		return result;
	}
	
	public boolean receiveOk(int requesterTimestamp) {
		System.out.println("receiveOk------Server talking: got OK");
		ServerSide.updateTimestamp(requesterTimestamp);
		System.out.println("TIC-TAC (receive operation " + requesterTimestamp + ")" + ServerSide.getTimestamp());
		if(ServerSide.checkState("wanted")) {
			ServerSide.addAccessAnswer();
			if(ServerSide.hasAccess())
				ServerSide.calculate(value);
		}
		return true;
	}
}
