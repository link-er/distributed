package distributed;

import java.util.*;

public class DistributedServer {
	private static int value;
	public static void setNewValue(int newValue) {
		value = newValue;
	}
	public static int getValue() {
		return value;
	}
	
//	waiting for url in form http://server:port/pds/
	public String addNode(String joiningAddress) {
		System.out.println(Helper.logStart(0) + joiningAddress + " wants to join us");
//		adding ourself only if it the first call for join and we have nothing in net
		ServerSide.ownHostFirstInitialize();
//		calling internal node method to add the joining host to net and remember it
		ServerSide.addHost(joiningAddress);
		System.out.println(Helper.logStart(0) + Arrays.toString(ServerSide.getNetSimpleArray()));
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
		System.out.println(Helper.logStart(0) + offAddress + " wants to sign off");
//		calling internal node method to remove the host from net
		ServerSide.removeHost(offAddress);
		System.out.println(Helper.logStart(0) + Arrays.toString(ServerSide.getNetSimpleArray()));
		return true;
	}
	
	Thread thread = new Thread(){
		public void run(){
			ServerSide.startTimestamp();
			Random rand = new Random();
			long startTime = System.currentTimeMillis();
			while(System.currentTimeMillis() - startTime <= 20 * 1000) {
				int timeToSleep, operationToMake, operand;
				timeToSleep = rand.nextInt(2);
				try {
					System.out.println(Helper.logStart(ServerSide.getTimestamp()) + "sleep for " + timeToSleep + "s.");
					Thread.sleep(timeToSleep*1000);
				} catch (InterruptedException e) {
					System.out.println("Cannot sleep");
				}
				ServerSide.requestAccess();
				operationToMake = rand.nextInt(4);
//				in order not to check if we got 0 for division
				operand = rand.nextInt(5) + 1;
				ServerSide.queueOperation(operationToMake, operand);
//				wanted state could be finished by receiveOK or receiveToken
				if(ServerSide.checkState("wanted") && ServerSide.hasAccess())
					ServerSide.calculate(value);
			}
			ServerSide.finishQueue(value);
			System.out.println(Helper.logStart(ServerSide.getTimestamp()) + "finished, got ***" + value + "***");
			System.out.println("***FINISH***");
			ServerSide.setDone();
		}
	};
	
	public boolean calculateValue(int initialValue) {
		value = initialValue;
		System.out.println("***START***");
		System.out.println(Helper.logStart(0) + "starting with initial value " + value);
		thread.start();
		return true;
	}
	
	public boolean performOperation(int operation, int operand, int requesterTimestamp) {
		System.out.println(Helper.logStart(ServerSide.getTimestamp()) + "receive event " + requesterTimestamp);
		ServerSide.updateTimestamp(requesterTimestamp);
		int startValue = value;
		value = new ServerSide.Operation(operation, operand).implement(value);
		System.out.println(Helper.logStart(ServerSide.getTimestamp()) +"called for making on " + startValue + " operation " +
				operation + " with " + operand + " and got result ***" + value + "***");
		return true;
	}
	
	public boolean done(String doneHost) {
		System.out.println(Helper.logStart(ServerSide.getTimestamp()) +doneHost + " finished calculations.");
//		calling internal node method to add the host to done
		ServerSide.addDoneHost(doneHost);
		return true;
	}
	
//	===========TOKEN RING
	Thread tokenCalculation = new Thread() { 
		public void run(){
			if(ServerSide.hasAccess()) {
				if(ServerSide.checkState("wanted"))
					ServerSide.calculate(value);
				else {
					if(ServerSide.checkState("free")) {
						ServerSide.freeResource();
					}
				}
			}
		}
	};
	
	public boolean receiveToken() {
		System.out.println(Helper.logStart(0) + "got token");
		ServerSide.getToken();
		tokenCalculation.start();
		return true;
	}
	
//=============ARGAWALA
	public boolean receiveRequest(String nodeId, int wantedTimestamp, int requesterTimestamp) {
		System.out.println(Helper.logStart(ServerSide.getTimestamp()) + "receive event " + requesterTimestamp);
		boolean result;
		if(ServerSide.checkState("free")) {
			System.out.println(Helper.logStart(ServerSide.getTimestamp()) + "state is free, return OK access to " + nodeId);
			result = true;
		}
		else {
			if((ServerSide.checkState("wanted") && (wantedTimestamp > ServerSide.getWantedTimestamp() ||
					(wantedTimestamp == ServerSide.getWantedTimestamp() && nodeId.compareTo(ServerSide.getOwnHostAddress()) > 0))) ||
					ServerSide.checkState("locked")) {
				ServerSide.enque(nodeId);
				System.out.println(Helper.logStart(ServerSide.getTimestamp()) + "state is not free, return no access to " + nodeId);
				result = false;
			}
			else {
				System.out.println(Helper.logStart(ServerSide.getTimestamp()) + "state is not free, but return access OK to " + nodeId);
				result = true;
			}
		}
		ServerSide.updateTimestamp(requesterTimestamp);
		return result;
	}
	
	public boolean receiveOk(int requesterTimestamp) {
		System.out.println(Helper.logStart(ServerSide.getTimestamp()) + "receive event " + requesterTimestamp);
		ServerSide.updateTimestamp(requesterTimestamp);
		System.out.println(Helper.logStart(ServerSide.getTimestamp()) + "got OK");
//		has to be if we are here, but for safety
		if(ServerSide.checkState("wanted")) {
			ServerSide.addAccessAnswer();
			if(ServerSide.hasAccess())
				ServerSide.calculate(value);
		}
		return true;
	}
}
