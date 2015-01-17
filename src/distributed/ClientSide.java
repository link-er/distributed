package distributed;

import java.net.MalformedURLException;
import java.net.URL;
import org.apache.xmlrpc.client.*;

public class ClientSide {
	private URL requestHost;
	public XmlRpcClient sender;
	
	public ClientSide(String hostAddress) throws MalformedURLException {
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		requestHost = new URL(hostAddress);
		config.setServerURL(requestHost);
		sender = new XmlRpcClient();
		sender.setConfig(config);
	}
	
	public ClientSide(URL host) {
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		requestHost = host;
		config.setServerURL(requestHost);
		sender = new XmlRpcClient();
		sender.setConfig(config);
	}
}
