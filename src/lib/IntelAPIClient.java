package lib;

import java.io.IOException;
import java.net.Proxy;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.Authenticator;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;

public class IntelAPIClient {

	private final static String BASE_URL = "https://intel.ingress.com/r/getEntities";

	static final TrustManager[] trustAllCerts = new TrustManager[] {
			new X509TrustManager() {
				@Override
				public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}

				@Override
				public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}

				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return new java.security.cert.X509Certificate[] {};
				}
			}
	};
	private static final SSLContext trustAllSslContext;

	static {
		try {
			trustAllSslContext = SSLContext.getInstance("SSL");
			trustAllSslContext.init(null, trustAllCerts, new java.security.SecureRandom());
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			throw new RuntimeException(e);
		}
	}

	static final SSLSocketFactory trustAllSslSocketFactory = trustAllSslContext.getSocketFactory();

	private String csrfToken, sessionId, apiVersion;
	private Proxy proxy;
	private Authenticator proxyAuthenticator;

	private IntelAPIClient(String csrfToken, String sessionId, String apiVersion) {
		this.csrfToken = csrfToken;
		this.sessionId = sessionId;
		this.apiVersion = apiVersion;
	}

	private IntelAPIClient(String csrfToken, String sessionId, String apiVersion, Proxy proxy, String proxyUsername, String proxyPassword) {
		this.csrfToken = csrfToken;
		this.sessionId = sessionId;
		this.apiVersion = apiVersion;

		this.proxy = proxy;
		if (proxyUsername != null && proxyPassword != null) {
			this.proxyAuthenticator = new Authenticator() {
				@Override
				public Request authenticate(Route route, Response response) throws IOException {
					String credential = Credentials.basic(proxyUsername, proxyPassword);
					return response.request().newBuilder()
							.header("Proxy-Authorization", credential)
							.build();
				}
			};
		}
	}

	public static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

	public synchronized List<IngressPortal> getPortalsAround(double lat, double lng, boolean orderByDistance) {
		OkHttpClient client = getHTTPClient(lat, lng);
		Set<TileKey> tileKeysInArea = getTileKeysAround(lat, lng);
		String tilesString = StringUtils.join(tileKeysInArea, "\",\"");

		String requestString = "{\"tileKeys\":[\"" + tilesString + "\"]"
				+ ",\"v\":\"" + apiVersion + "\"}";
		System.out.println("Intel requestString: " + requestString);
		RequestBody requestBody = RequestBody.Companion.create(requestString, JSON_MEDIA_TYPE);

		Request request = new Request.Builder()
				.url(BASE_URL)
				.post(requestBody)
				.addHeader("x-csrftoken", csrfToken)
				.addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.142 Safari/537.36")
				.addHeader("referer", "https://intel.ingress.com/")
				.addHeader("origin", "https://intel.ingress.com")
				.addHeader("content-type", "application/json")
				.build();

		List<IngressPortal> portals = new ArrayList<>();

		try {
			Response response = client.newCall(request).execute();
			String responseString;
			int code = response.code();
			if (code == 200) {
				if (response.body() != null) {
					responseString = response.body().string();
					System.out.println("Response: " + responseString);

					try {
						JSONObject json = new JSONObject(responseString);

						JSONObject map = json.getJSONObject("result").getJSONObject("map");
						Iterator<String> keys = map.keys();
						while (keys.hasNext()) {
							String key = keys.next();
							if (map.get(key) instanceof JSONObject) {
								JSONObject list = map.getJSONObject(key);
								if (!list.isNull("error")) {
									System.out.println("Error with tile " + key + " -> " + list.optString("error"));
									continue;
								}
								JSONArray entities = list.getJSONArray("gameEntities");
								int size = entities.length();
								for (int i = 0; i < size; i++) {
									JSONArray array = entities.getJSONArray(i);
									String id = array.getString(0);

									JSONArray info = array.getJSONArray(2);
									if (info.getString(0).equals("p")) {
										String name = info.getString(8);
										// String imgUrl = info.getString(7);
										double latitude = info.getInt(2) / 1_000_000d;
										double longitude = info.getInt(3) / 1_000_000d;
										/* ["bf1f6790401b4a9fa5a8c4b22a374d4c.16",
										 * 1562908068848,
										 * ["p",
										 * "N",
										 * 45655004,
										 * 8787447,
										 * 1,
										 * 0,
										 * 0,
										 * "http://lh5.ggpht.com/qD_FKU2v8zyhSq21EMtgCAGUzOB5uK6lAQMeFc5rOZvbusUNeKW3ghLm8SYGc4pRDSUagztvCYb9DLvjoYk"
										 * ,"Graffiti"
										 * ,[]
										 * ,false
										 * ,false
										 * ,null
										 * ,1562908068848]] */
										portals.add(new IngressPortal(id, name, latitude, longitude));
									}
								}
							}
						}
					} catch (Exception e) {
						System.out.println("calling Intel Ingress (internal), response (code " + code + "): '" + responseString + "'");
						e.printStackTrace();
					}
				} else {
					System.out.println("Intel response is null!");
				}
			} else {
				System.out.println("unsuccessful response code from Intel: " + code);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (orderByDistance) {
			portals.sort(new Comparator<IngressPortal>() {
				@Override
				public int compare(IngressPortal p1, IngressPortal p2) {
					Double distance1 = Haversine.distance(lat, lng, p1.getLatitude(), p1.getLongitude());
					Double distance2 = Haversine.distance(lat, lng, p2.getLatitude(), p2.getLongitude());
					return distance1.compareTo(distance2);
				}
			});
		}

		return portals;
	}

	private Set<TileKey> getTileKeysAround(double lat, double lng) {
		TileKey base = new TileKey(lat, lng);

		return new HashSet<>(Arrays.asList(
				base,
				new TileKey(base, -1, -1),
				new TileKey(base, -1, 0),
				new TileKey(base, -1, 1),
				new TileKey(base, 0, -1),
				new TileKey(base, 0, 1),
				new TileKey(base, 1, 0),
				new TileKey(base, 1, 1),
				new TileKey(base, 1, -1)));
	}

	private OkHttpClient getHTTPClient(double lat, double lng) {
		CookieJar cookie = new CookieJar() {
			@Override
			public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {}

			@Override
			public List<Cookie> loadForRequest(HttpUrl url) {
				List<Cookie> cookies = new ArrayList<>();
				cookies.add(buildIngressCookie("csrftoken", csrfToken, "r"));
				cookies.add(buildIngressCookie("sessionid", sessionId, "r"));

				cookies.add(buildIngressCookie("__utmz", "139770731.1563612881.6.5.utmcsr=google|utmccn=(organic)|utmcmd=organic|utmctr=https://intel.ingress.com/", "r"));
				cookies.add(buildIngressCookie("__utma", "139770731.2017418329.1560616270.1564153096.1564240565.23", "r"));
				cookies.add(buildIngressCookie("__utmc", "139770731; ingress.intelmap.shflt=viz", "r"));
				cookies.add(buildIngressCookie("__utmt", "1", "r"));
				cookies.add(buildIngressCookie("__utmb", "139770731.10.9.1564241555710", "r"));
				cookies.add(buildIngressCookie("ingress.intelmap.lat", "" + lat, "r"));
				cookies.add(buildIngressCookie("ingress.intelmap.lng", "" + lng, "r"));
				cookies.add(buildIngressCookie("ingress.intelmap.zoom", "" + TileKey.DEFAULT_ZOOM, "r"));

				cookies.add(buildIngressCookie("csrftoken", csrfToken, ""));
				cookies.add(buildIngressCookie("sessionid", sessionId, ""));

				cookies.add(buildIngressCookie("__utmz", "139770731.1563612881.6.5.utmcsr=google|utmccn=(organic)|utmcmd=organic|utmctr=https://intel.ingress.com/", ""));
				cookies.add(buildIngressCookie("__utma", "139770731.2017418329.1560616270.1564153096.1564240565.23", ""));
				cookies.add(buildIngressCookie("__utmc", "139770731; ingress.intelmap.shflt=viz", ""));
				cookies.add(buildIngressCookie("__utmt", "1", ""));
				cookies.add(buildIngressCookie("__utmb", "139770731.10.9.1564241555710", ""));
				cookies.add(buildIngressCookie("ingress.intelmap.lat", "" + lat, ""));
				cookies.add(buildIngressCookie("ingress.intelmap.lng", "" + lng, ""));
				cookies.add(buildIngressCookie("ingress.intelmap.zoom", "" + TileKey.DEFAULT_ZOOM, ""));

				return cookies;
			}
		};

		OkHttpClient.Builder builder = new OkHttpClient.Builder()
				.connectTimeout(15, TimeUnit.SECONDS)
				.writeTimeout(15, TimeUnit.SECONDS)
				.readTimeout(15, TimeUnit.SECONDS)
				.sslSocketFactory(trustAllSslSocketFactory, (X509TrustManager) trustAllCerts[0])
				.cookieJar(cookie)
				.hostnameVerifier((hostname, session) -> true);
		if (proxy != null) {
			builder.setProxy$okhttp(proxy);
			if (proxyAuthenticator != null) {
				builder.setProxyAuthenticator$okhttp(proxyAuthenticator);
			}
		}

		OkHttpClient client = builder.build();
		return client;
	}

	private static Cookie buildIngressCookie(String name, String value, String path) {
		return new Cookie.Builder().name(name).value(value).domain("intel.ingress.com").path("/" + path).build();
	}

	public static class Builder {

		private String csrfToken, sessionId, apiVersion;
		private Proxy proxy;
		private String proxyUsername, proxyPassword;

		public Builder() {}

		public Builder withCsfrToken(String token) {
			this.csrfToken = token;
			return this;
		}

		public Builder withSessionId(String sessionId) {
			this.sessionId = sessionId;
			return this;
		}

		public Builder withApiVersion(String apiVersion) {
			this.apiVersion = apiVersion;
			return this;
		}

		public Builder withProxy(Proxy proxy) {
			this.proxy = proxy;
			return this;
		}

		public Builder withProxyCredentials(String proxyUsername, String proxyPassword) {
			this.proxyUsername = proxyUsername;
			this.proxyPassword = proxyPassword;
			return this;
		}

		public IntelAPIClient build() throws Exception {
			if (csrfToken == null || sessionId == null || apiVersion == null) {
				throw new Exception("Cannot initialize IntelAPIClient without csrfToken, session id or API version, please use withCsfrToken(token), withSessionId(sessionId) and withApiVersion(apiVersion) on Builder before building");
			}
			if (proxy != null) {
				return new IntelAPIClient(csrfToken, sessionId, apiVersion, proxy, proxyUsername, proxyPassword);
			} else {
				return new IntelAPIClient(csrfToken, sessionId, apiVersion);
			}
		}
	}
}
