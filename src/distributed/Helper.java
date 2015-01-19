package distributed;

public class Helper {
	public static String formUrlString(String hostAddress) {
		return "http://" + hostAddress + "/pds/";
	}
	
	public static String logStart(int time) {
		return "Thread" + Thread.currentThread().getId() + ", " + time + " tacks: ";
	}
}
