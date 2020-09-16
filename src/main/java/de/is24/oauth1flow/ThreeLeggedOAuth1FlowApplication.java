package de.is24.oauth1flow;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth.common.signature.SharedConsumerSecretImpl;
import org.springframework.security.oauth.consumer.BaseProtectedResourceDetails;
import org.springframework.security.oauth.consumer.OAuthConsumerSupport;
import org.springframework.security.oauth.consumer.OAuthConsumerToken;
import org.springframework.security.oauth.consumer.ProtectedResourceDetails;
import org.springframework.security.oauth.consumer.client.CoreOAuthConsumerSupport;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@RestController
class TestController {
    private static final String SANDBOX_URL = "https://rest.sandbox-immobilienscout24.de";
    public static final String REQUEST_TOKEN_URL = SANDBOX_URL + "/restapi/security/oauth/request_token";
    public static final String ACCESS_TOKEN_URL = SANDBOX_URL + "/restapi/security/oauth/access_token";
    public static final String ACCESS_CONFIRMATION_URL = SANDBOX_URL + "/restapi/security/oauth/confirm_access";
    private static final String RESOURCE_ENDPOINT_URL = SANDBOX_URL + "/restapi/api/offer/v1.0/user/me/realestate/";

    private static final String CLIENT_KEY = "client-key";
    private static final String CLIENT_SECRET = "client-secret";

    private static final String IS24_SANDBOX = "is24-sandbox";

    @Value("http://localhost:${server.port}/callback")
    String callbackUrl;
    WebClient webClient = WebClient.builder().build();
    OAuthConsumerSupport oAuthConsumerSupport = new CoreOAuthConsumerSupport();
    ProtectedResourceDetails is24ResourceDetails = createIs24ResourceDetails();

    Map<String, OAuthConsumerToken> requestTokenRepository = new HashMap<>();
    Map<String, OAuthConsumerToken> accessTokenRepository = new HashMap<>();

    ProtectedResourceDetails createIs24ResourceDetails() {
        BaseProtectedResourceDetails protectedResourceDetails = new BaseProtectedResourceDetails();
        protectedResourceDetails.setConsumerKey(CLIENT_KEY);
        protectedResourceDetails.setSharedSecret(new SharedConsumerSecretImpl(CLIENT_SECRET));
        protectedResourceDetails.setSignatureMethod("HMAC-SHA1");
        protectedResourceDetails.setAccessTokenURL(ACCESS_TOKEN_URL);
        protectedResourceDetails.setRequestTokenURL(REQUEST_TOKEN_URL);
        protectedResourceDetails.setId(IS24_SANDBOX);

        return protectedResourceDetails;
    }

    @GetMapping("/initialize-token-exchange")
    public void initializeTokenExchange(HttpServletResponse response, Authentication yourLocalUserAuthentication) throws IOException {
        String userName = yourLocalUserAuthentication.getName();
        OAuthConsumerToken requestToken = oAuthConsumerSupport.getUnauthorizedRequestToken(is24ResourceDetails, callbackUrl);
        requestTokenRepository.put(userName, requestToken);
        response.sendRedirect(ACCESS_CONFIRMATION_URL + "?oauth_token=" + requestToken.getValue());
    }

    @GetMapping("/callback")
    public void oauthCallback(@RequestParam("state") String state,
                              @RequestParam("oauth_token") String requestToken,
                              @RequestParam("oauth_verifier") String verifier,
                              Authentication yourLocalUserAuthentication,
                              HttpServletResponse response) throws IOException {
        String userName = yourLocalUserAuthentication.getName();
        OAuthConsumerToken currentRequestToken = requestTokenRepository.get(userName);

        if (currentRequestToken == null || !currentRequestToken.getValue().equals(requestToken)) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Request token for current user does not match token from request!");
            return;
        }
        if (!"authorized".equals(state)) {
            // error
        }

        OAuthConsumerToken accessToken = oAuthConsumerSupport.getAccessToken(is24ResourceDetails, currentRequestToken, verifier);
        accessTokenRepository.put(userName, accessToken);
    }

    @GetMapping(value = "/load-real-estates", produces = "text/plain")
    public String loadRealEstates(Authentication yourLocalUserAuthentication) throws IOException, URISyntaxException {
        String userName = yourLocalUserAuthentication.getName();
        OAuthConsumerToken accessToken = accessTokenRepository.get(userName);
        if (accessToken == null) {
            // initialize token exchange
        }
        URL url = new URL(RESOURCE_ENDPOINT_URL);
        String authHeader = oAuthConsumerSupport.getAuthorizationHeader(is24ResourceDetails, accessToken, url, "GET", null);
        return webClient.get()
                .uri(url.toURI())
                .header("Authorization", authHeader)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}

@SpringBootApplication
public class ThreeLeggedOAuth1FlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThreeLeggedOAuth1FlowApplication.class, args);
    }
}
