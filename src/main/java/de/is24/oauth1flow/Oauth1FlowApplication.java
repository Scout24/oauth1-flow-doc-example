package de.is24.oauth1flow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth.common.signature.SharedConsumerSecretImpl;
import org.springframework.security.oauth.consumer.BaseProtectedResourceDetails;
import org.springframework.security.oauth.consumer.InMemoryProtectedResourceDetailsService;
import org.springframework.security.oauth.consumer.OAuthConsumerSupport;
import org.springframework.security.oauth.consumer.OAuthConsumerToken;
import org.springframework.security.oauth.consumer.ProtectedResourceDetails;
import org.springframework.security.oauth.consumer.ProtectedResourceDetailsService;
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
    @Autowired
    WebClient webClient;
    @Autowired
    OAuthConsumerSupport oAuthConsumerSupport;
    @Autowired
    ProtectedResourceDetails is24ResourceDetails;

    Map<String, OAuthConsumerToken> requestTokenRepository = new HashMap<>();
    Map<String, OAuthConsumerToken> accessTokenRespository = new HashMap<>();

    @Bean
    ProtectedResourceDetailsService protectedResourceDetailsService(ProtectedResourceDetails is24ResourceDetails) {
        InMemoryProtectedResourceDetailsService inMemoryProtectedResourceDetailsService = new InMemoryProtectedResourceDetailsService();
        HashMap<String, ProtectedResourceDetails> detailsServiceHashMap = new HashMap<>();
        detailsServiceHashMap.put(IS24_SANDBOX, is24ResourceDetails);
        inMemoryProtectedResourceDetailsService.setResourceDetailsStore(detailsServiceHashMap);

        return inMemoryProtectedResourceDetailsService;
    }

    @Bean
    ProtectedResourceDetails is24ResourceDetails() {
        BaseProtectedResourceDetails protectedResourceDetails = new BaseProtectedResourceDetails();
        protectedResourceDetails.setConsumerKey(CLIENT_KEY);
        protectedResourceDetails.setSharedSecret(new SharedConsumerSecretImpl(CLIENT_SECRET));
        protectedResourceDetails.setSignatureMethod("HMAC-SHA1");
        protectedResourceDetails.setAccessTokenURL(ACCESS_TOKEN_URL);
        protectedResourceDetails.setRequestTokenURL(REQUEST_TOKEN_URL);
        protectedResourceDetails.setId(IS24_SANDBOX);

        return protectedResourceDetails;
    }

    @Bean
    public OAuthConsumerSupport oAuthConsumerSupport() {
        return new CoreOAuthConsumerSupport();
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }

    @GetMapping("/initialize-token-exchange")
    public void initializeTokenExchange(HttpServletResponse response, Authentication authentication) throws IOException {
        String userName = authentication.getName();
        OAuthConsumerToken requestToken = oAuthConsumerSupport.getUnauthorizedRequestToken(is24ResourceDetails, callbackUrl);
        requestTokenRepository.put(userName, requestToken);
        response.sendRedirect(ACCESS_CONFIRMATION_URL + "?oauth_token=" + requestToken.getValue());
    }

    @GetMapping("/callback")
    public void callbackStuff(@RequestParam("state") String state,
                              @RequestParam("oauth_token") String requestToken,
                              @RequestParam("oauth_verifier") String verifier,
                              Authentication authentication,
                              HttpServletResponse response) throws IOException {
        // happy path
        String userName = authentication.getName();
        OAuthConsumerToken currentRequestToken = requestTokenRepository.get(userName);

        if (currentRequestToken == null || !currentRequestToken.getValue().equals(requestToken)) {
            response.sendRedirect("/error");
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Request token for current user does not match token from request!");
            return;
        }

        OAuthConsumerToken accessToken = oAuthConsumerSupport.getAccessToken(is24ResourceDetails, currentRequestToken, verifier);
        accessTokenRespository.put(userName, accessToken);

        response.sendRedirect("/make-request");
    }

    @GetMapping(value = "/make-request", produces = "text/plain")
    public String makeRequest(Authentication authentication) throws IOException, URISyntaxException {
        String userName = authentication.getName();
        OAuthConsumerToken accessToken = accessTokenRespository.get(userName);
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
public class Oauth1FlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(Oauth1FlowApplication.class, args);
    }
}
