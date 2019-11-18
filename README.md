# IntelJavaAPI
Use this lib to call Intel Ingress API (by Niantic) and download portals around a given location.

src/Test.java contains a simple example.

How to:
- log in to your Intel account (https://intel.ingress.com/) with Google
- open Developers tools of your browser and copy the following from any call to GetEntities:
  - CsfrToken
  - API version (named "v")
  - SessionId (available in cookies)
- insert these values in the code here below and enjoy.

NB: session ID expires in 2 weeks! You have to update it manually. If you know how to login, please tell me :)
NB2: API version is updated from time to time. If you receive an error, please check it isn't changed.

------------------------------

import java.util.List;

import lib.IngressPortal;
import lib.IntelAPIClient;

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
