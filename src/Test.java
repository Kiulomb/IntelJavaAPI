import java.util.List;

import lib.IngressPortal;
import lib.IntelAPIClient;

public class Test {

	public static void main(String[] args) throws Exception {
		// My suggestion:
		// Intel returns timeouts very frequently.
		// For more accurate results, re-iterate failed cells until everything is done
		
		IntelAPIClient intelAPIClient = new IntelAPIClient.Builder()
				.withCsfrToken("xxx")
				.withSessionId("yyy")	
				.withApiVersion("zzz")			
				.build();

		List<IngressPortal> portals = intelAPIClient.getPortalsAround(45.7, 8.7, true);
		System.out.println("Found " + portals.size() + " portals");
		for (IngressPortal ingressPortal : portals) {
			System.out.println(ingressPortal);
		}
	}
}