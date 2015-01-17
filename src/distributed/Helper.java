package distributed;

public class Helper {
	public static String formUrlString(String hostAddress) {
		return "http://" + hostAddress + "/pds/";
	}
}
