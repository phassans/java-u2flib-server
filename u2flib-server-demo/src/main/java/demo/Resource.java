package demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.yubico.u2f.U2F;
import com.yubico.u2f.attestation.MetadataService;
import com.yubico.u2f.data.DeviceRegistration;
import com.yubico.u2f.data.messages.AuthenticateRequestData;
import com.yubico.u2f.data.messages.RegisterRequestData;
import com.yubico.u2f.exceptions.NoEligibleDevicesException;

import demo.view.AuthenticationView;
import demo.view.RegistrationView;
import io.dropwizard.views.View;

@Path("/")
@Produces(MediaType.TEXT_HTML)
public class Resource {

    public static final String APP_ID = "https://localhost:8443";
    public static String requestID = "";
    public static final String NAVIGATION_MENU = "<h2>Navigation</h2><ul><li><a href='/assets/registerIndex.html'>Register</a></li><li><a href='/assets/loginIndex.html'>Login</a></li></ul>";

    private final Map<String, String> requestStorage = new HashMap<String, String>();
    private final LoadingCache<String, Map<String, String>> userStorage = CacheBuilder.newBuilder()
        .build(new CacheLoader<String, Map<String, String>>() {
            @Override
            public Map<String, String> load(String key) throws Exception {
                return new HashMap<String, String>();
            }
        });
    private final U2F u2f = new U2F();
    private final MetadataService metadataService = new MetadataService();

    @Path("startRegistration")
    @GET
    public View startRegistration(@QueryParam("username") String username)
        throws ClientProtocolException, IOException, ParseException {
        /*
         * RegisterRequestData registerRequestData =
         * u2f.startRegistration(APP_ID, getRegistrations(username));
         * System.out.println("DEBUG startRegistration 0: " +
         * getRegistrations(username));
         * System.out.println("DEBUG startRegistration 1: " +
         * registerRequestData.toJson());
         * requestStorage.put(registerRequestData.getRequestId(),
         * registerRequestData.toJson());
         */

        // RegisterRequestData registerRequestData =
        // u2f.startRegistration(APP_ID, getRegistrations(username));
        // requestStorage.put(registerRequestData.getRequestId(),
        // registerRequestData.toJson());

        DefaultHttpClient httpClient = new DefaultHttpClient();
        httpClient = (DefaultHttpClient) WebClientDevWrapper.wrapClient(httpClient);
        HttpPost postRequest = new HttpPost(
            "https://identity2faserv7949.ccg21.dev.paypalcorp.com:15713/v1/identity/u2f-authenticators/begin-registration");

        StringEntity input = new StringEntity("");
        input.setContentType("application/json");
        postRequest.setEntity(input);
        postRequest.setHeader("X-PAYPAL-SECURITY-CONTEXT",
            "{\"subjects\":[{\"subject\":{\"public_credential\":\"TWOFAUSER01\",\"id\":\"11339226\",\"account_number\":\"2226156280263819193\",\"party_id\":\"1984903268342642250\",\"user_type\":\"CONSUMER\"}}]}");

        HttpResponse response = httpClient.execute(postRequest);

        if (response.getStatusLine().getStatusCode() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
        }

        BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));

        String output = br.readLine();

        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(output);

        requestID = (String) jsonObject.get("requestId");

        System.out.println("requestId " + requestID);

        // httpClient.getConnectionManager().shutdown();

        // return new RegistrationView(requestID, username);

        return new RegistrationView(
            "{\"authenticateRequests\":[],\"registerRequests\":[{\"challenge\":\"llHvQoSCF3s_mudJ8x0nFjxjtC895tKxLYriHpqF1WQ\",\"appId\":\"https://localhost:8443\",\"version\":\"U2F_V2\"}]}",
            username);
    }

    @Path("finishRegistration")
    @POST
    public String finishRegistration(@FormParam("tokenResponse") String tokenResponse,
        @FormParam("username") String username)
        throws CertificateException, NoSuchFieldException, ClientProtocolException, IOException, ParseException {

        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(tokenResponse);

        jsonObject.put("requestId", requestID);

        System.out.println("JsonResponse " + jsonObject.toJSONString());

        // JSONObject obj = new JSONObject();
        // obj.put(key, value);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        httpClient = (DefaultHttpClient) WebClientDevWrapper.wrapClient(httpClient);
        HttpPost postRequest = new HttpPost(
            "https://identity2faserv7949.ccg21.dev.paypalcorp.com:15713/v1/identity/u2f-authenticators/finish-registration");

        StringEntity input = new StringEntity(jsonObject.toJSONString());
        input.setContentType("application/json");
        postRequest.setEntity(input);
        postRequest.setHeader("X-PAYPAL-SECURITY-CONTEXT",
            "{\"subjects\":[{\"subject\":{\"public_credential\":\"TWOFAUSER01\",\"id\":\"11339226\",\"account_number\":\"2226156280263819193\",\"party_id\":\"1984903268342642250\",\"user_type\":\"CONSUMER\"}}]}");

        HttpResponse response = httpClient.execute(postRequest);

        if (response.getStatusLine().getStatusCode() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
        }

        BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));

        String output;
        System.out.println("Output from Server .... \n");
        while ((output = br.readLine()) != null) {
            System.out.println(output);
        }

        httpClient.getConnectionManager().shutdown();

        /*
         * RegisterResponse registerResponse =
         * RegisterResponse.fromJson(response); RegisterRequestData
         * registerRequestData = RegisterRequestData
         * .fromJson(requestStorage.remove(registerResponse.getRequestId()));
         * DeviceRegistration registration =
         * u2f.finishRegistration(registerRequestData, registerResponse);
         * System.out.println("DEBUG finishRegistration 0: "+registerResponse.
         * toJson()); Attestation attestation =
         * metadataService.getAttestation(registration.getAttestationCertificate
         * ());
         * 
         * addRegistration(username, registration); StringBuilder buf = new
         * StringBuilder();
         * buf.append("<p>Successfully registered device:</p>"); if
         * (!attestation.getVendorProperties().isEmpty()) {
         * buf.append("<p>Vendor metadata</p><pre>"); for (Map.Entry<String,
         * String> entry : attestation.getVendorProperties().entrySet()) {
         * buf.append(entry.getKey()).append(": ").append(entry.getValue()).
         * append("\n"); } buf.append("</pre>"); } else {
         * buf.append("<p>No vendor metadata present!</p>"); } if
         * (!attestation.getDeviceProperties().isEmpty()) {
         * buf.append("<p>Device metadata</p><pre>"); for (Map.Entry<String,
         * String> entry : attestation.getDeviceProperties().entrySet()) {
         * buf.append(entry.getKey()).append(": ").append(entry.getValue()).
         * append("\n"); } buf.append("</pre>"); } else {
         * buf.append("<p>No device metadata present!</p>"); } if
         * (!attestation.getTransports().isEmpty()) {
         * buf.append("<p>Device transports: ").append(attestation.getTransports
         * ()).append("</p>"); } else {
         * buf.append("<p>No device transports reported!</p>"); }
         * buf.append("<p>Registration data</p><pre>").append(registration).
         * append("</pre>").append(NAVIGATION_MENU);
         */

        return null;
    }

    @Path("startAuthentication")
    @GET
    public View startAuthentication(@QueryParam("username") String username) throws NoEligibleDevicesException {
        AuthenticateRequestData authenticateRequestData = u2f.startAuthentication(APP_ID, getRegistrations(username));
        System.out.println("DEBUG startAuthentication 0: " + getRegistrations(username));
        System.out.println("DEBUG startAuthentication 1: " + authenticateRequestData.toJson());
        System.out.println("DEBUG startAuthentication 2: " + authenticateRequestData.getRequestId());

        requestStorage.put(authenticateRequestData.getRequestId(), authenticateRequestData.toJson());
        return new AuthenticationView(authenticateRequestData.toJson(), username);
    }

    @Path("finishAuthentication")
    @POST
    public String finishAuthentication(@FormParam("tokenResponse") String tokenResponse,
        @FormParam("username") String username) throws ClientProtocolException, IOException {

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost postRequest = new HttpPost(
            "https://identity2faserv7949.ccg21.dev.paypalcorp.com:15713/v1/identity/u2f-authenticators/finish-registration");

        StringEntity input = new StringEntity(tokenResponse);
        input.setContentType("application/json");
        postRequest.setEntity(input);

        HttpResponse response = httpClient.execute(postRequest);

        if (response.getStatusLine().getStatusCode() != 201) {
            throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
        }

        BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));

        String output;
        System.out.println("Output from Server .... \n");
        while ((output = br.readLine()) != null) {
            System.out.println(output);
        }

        httpClient.getConnectionManager().shutdown();

        /*
         * AuthenticateResponse authenticateResponse =
         * AuthenticateResponse.fromJson(response);
         * System.out.println("DEBUG finishAuthentication 1: "
         * +authenticateResponse.toJson()); AuthenticateRequestData
         * authenticateRequest = AuthenticateRequestData
         * .fromJson(requestStorage.remove(authenticateResponse.getRequestId()))
         * ; DeviceRegistration registration = null; try { registration =
         * u2f.finishAuthentication(authenticateRequest, authenticateResponse,
         * getRegistrations(username)); } catch (DeviceCompromisedException e) {
         * registration = e.getDeviceRegistration(); return
         * "<p>Device possibly compromised and therefore blocked: " +
         * e.getMessage() + "</p>" + NAVIGATION_MENU; } finally {
         * userStorage.getUnchecked(username).put(registration.getKeyHandle(),
         * registration.toJson()); }
         */
        return "<p>Successfully authenticated!<p>" + NAVIGATION_MENU;
    }

    private Iterable<DeviceRegistration> getRegistrations(String username) {
        List<DeviceRegistration> registrations = new ArrayList<DeviceRegistration>();
        for (String serialized : userStorage.getUnchecked(username).values()) {
            registrations.add(DeviceRegistration.fromJson(serialized));
        }
        return registrations;
    }

    private void addRegistration(String username, DeviceRegistration registration) {
        userStorage.getUnchecked(username).put(registration.getKeyHandle(), registration.toJson());
    }
}
