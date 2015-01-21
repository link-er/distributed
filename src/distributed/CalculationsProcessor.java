package distributed;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.apache.xmlrpc.XmlRpcException;

public class CalculationsProcessor {
	public static void main(String[] args) {
		ClientSide client = null;
		Object[] params;
		
		Scanner in = new Scanner(System.in);
		System.out.println("***Hello***");
		System.out.println("Enter algorithm name to use (tr or ra):");
		String algorithm = in.nextLine();
		ServerSide.setAlgorithm(algorithm);
//		taking own IP address in form 192.168.1.1
		System.out.println("Please type in own IP address");
		String ownIP = in.nextLine();
//		making core server class with this IP
		ServerSide server = null;
		try {
			server = new ServerSide(ownIP);
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} catch (XmlRpcException e) {
			System.out.print("Failed to create server ");
			System.out.println(e.getMessage());
		}
		try {
			server.start();
		} catch (IOException e) {
			System.out.print("Failed to start server ");
			System.out.println(e.getMessage());
		}
		System.out.println("Server is up and running!");
		
		while(true) {
			System.out.println();
			System.out.println("Enter command or help for listing all possible commands:");
			String command = in.nextLine();
			switch(command) {
				case("help"): {
					System.out.println("Commands list:");
					System.out.println("(1) join - for joining calculative net, you will be asked for host address and port");
					System.out.println("(2) signOff - for signing off the calculative net");
					System.out.println("(3) start - to start distributed calculations, can be executed only if the host is in net");
					System.out.println("(4) exit - to stop program");
					break;
				}
				case("join"): {
					if(server.inNet()) {
						System.out.println("The machine is in the calculative net already! Type signOff if you want to rejoin.");
					}
					else {
						System.out.println("Enter host address in form 'IP:PORT', that already in the net:");
						String hostAddress = in.nextLine();
						
						String formedHostAddress = Helper.formUrlString(hostAddress);
						if(formedHostAddress.equals(ServerSide.getOwnHostAddress()))
							System.out.println("Cannot form calculative net with ourself! Type in another host!");
						else {
							try {
								client = new ClientSide(formedHostAddress);
							} catch (MalformedURLException e) {
								System.out.print("Failed to create client with entered URL ");
								System.out.println(e.getMessage());
							}
		//					sending as parameter own host address in full form http://address:port/pds/
							params = new Object[]{ServerSide.getOwnHostAddress()};
		//					getting the _ordered_ array of nodes in net, including asked node address
		//					_ordered_ mean that position in array is id of the server for TokenRing
							Object response = null;
							try {
								response = client.sender.execute("PDSProject.addNode", params);
							} catch (XmlRpcException e) {
								System.out.print("Failed to request server ");
								System.out.println(e.getMessage());
							}
							server.joinedToNet(response);
		//					propagating the join request to all nodes in net
							try {
								sendRequestToAll("PDSProject.addNode", params);
							} catch (XmlRpcException e) {
								System.out.print("Failed to broadcast join request ");
								System.out.println(e.getMessage());
							}
							System.out.print("We joined to network " + Arrays.toString(ServerSide.getNetSimpleArray()));
						}
					}
					
					break;
				}
				case("signOff"): {
					if(server.inNet()) {
						params = new Object[]{ServerSide.getOwnHostAddress()};
//						propagating to all nodes, that we want to sign off the net
						try {
							sendRequestToAll("PDSProject.disconnectNode", params);
						} catch (XmlRpcException e) {
							System.out.print("Failed to propagate sign off request ");
							System.out.println(e.getMessage());
						}
						server.signedOffNet();
						System.out.print("We signed off the network " + Arrays.toString(ServerSide.getNetSimpleArray()));
					}
					else {
						System.out.println("The machine is not in calculative net! Type command join first");
					}
					
					break;
				}
				case("start"): {
					if(server.inNet()) {
						ServerSide.setAlgorithm();
						try {
							client = new ClientSide(ServerSide.getOwnHostAddress());
						} catch (MalformedURLException e) {
							System.out.print("Failed to make client to own node ");
							System.out.println(e.getMessage());
						}
						Random rand = new Random();
						int initial = rand.nextInt(100);
//						sending initial value, used algorithm and structure of the net
						params = new Object[]{ initial };
						try {
							client.sender.execute("PDSProject.calculateValue", params);
							sendRequestToAll("PDSProject.calculateValue", params);
						} catch (XmlRpcException e) {
							System.out.print("Failed to request nodes to start calculations ");
							System.out.println(e.getMessage());
						}
					}
					else {
						System.out.println("The machine is not in calculative net! Type command join first");
					}
					break;
				}
				case("exit"): {
					server.stop();
					in.close();
					System.out.println();
					System.out.println("***Bye***");
					return;
				}
			}
		}
	}
	
	public static void sendRequestToAll(String request, Object[] params) throws XmlRpcException {
		URL[] broadcast = ServerSide.getNetBroadcast();
		for(URL netHost : broadcast) {
			ClientSide client = new ClientSide(netHost);
			client.sender.execute(request, params);
		}
	}
}
