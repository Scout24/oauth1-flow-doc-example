package de.is24;

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

    private static final String CLIENT_KEY = "your-client-key";
    private static final String CLIENT_SECRET = "your-client-secret";

    private static final String IS24_SANDBOX = "is24-sandbox";
    public static final String AUTHORIZED = "authorized";
    public static final String REJECTED = "rejected";

    @Value("http://localhost:${server.port}/callback")
    String callbackUrl;
    WebClient webClient = WebClient.builder().build();
    OAuthConsumerSupport oAuthConsumerSupport = new CoreOAuthConsumerSupport();
    ProtectedResourceDetails is24ClientKeyDetails = createIs24ClientKeyDetails();

    Map<String, OAuthConsumerToken> requestTokenRepository = new HashMap<>();
    Map<String, OAuthConsumerToken> accessTokenRepository = new HashMap<>();

    ProtectedResourceDetails createIs24ClientKeyDetails() {
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
        OAuthConsumerToken requestToken = oAuthConsumerSupport.getUnauthorizedRequestToken(is24ClientKeyDetails, callbackUrl);
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
        OAuthConsumerToken latestRequestTokenFromRepository = requestTokenRepository.get(userName);

        if (!isAuthorizedState(state)) {
            handleAccessConfirmationError(state, response);
            return;
        }
        if (!latestRequestTokenExistsAndMatchesTokenFromRequest(requestToken, latestRequestTokenFromRepository)) {
            handleInvalidRequestToken(response);
            return;
        }

        OAuthConsumerToken accessToken = oAuthConsumerSupport.getAccessToken(is24ClientKeyDetails, latestRequestTokenFromRepository, verifier);
        accessTokenRepository.put(userName, accessToken);
        redirectUserToPageOfYourChoice(response);
    }

    @GetMapping(value = "/load-real-estates", produces = "text/plain")
    public String loadRealEstates(Authentication yourLocalUserAuthentication, HttpServletResponse response) throws IOException, URISyntaxException {
        String userName = yourLocalUserAuthentication.getName();
        OAuthConsumerToken accessToken = accessTokenRepository.get(userName);

        if (accessToken == null) {
            response.sendRedirect("/initialize-token-exchange");
            return "Redirect";
        }

        URL url = new URL(RESOURCE_ENDPOINT_URL);
        String authHeader = oAuthConsumerSupport.getAuthorizationHeader(is24ClientKeyDetails, accessToken, url, "GET", null);
        return webClient.get()
                .uri(url.toURI())
                .header("Authorization", authHeader)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private boolean latestRequestTokenExistsAndMatchesTokenFromRequest(@RequestParam("oauth_token") String requestToken, OAuthConsumerToken latestRequestToken) {
        return latestRequestToken != null && latestRequestToken.getValue().equals(requestToken);
    }

    private void handleInvalidRequestToken(HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value(), "Request token for current user does not match token from request!");
    }

    private void handleAccessConfirmationError(String state, HttpServletResponse response) throws IOException {
        if (REJECTED.equals(state)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "The token was explicit not authorized by the user!");
        } else {
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An error has occurred during authorization!");
        }
    }

    private boolean isAuthorizedState(@RequestParam("state") String state) {
        return AUTHORIZED.equals(state);
    }

    private void redirectUserToPageOfYourChoice(HttpServletResponse response) throws IOException {
        response.sendRedirect("/load-real-estates");
    }
}

@SpringBootApplication
public class ThreeLeggedOAuth1FlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThreeLeggedOAuth1FlowApplication.class, args);
    }
}
