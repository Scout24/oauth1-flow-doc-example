package de.is24.oauth1flow;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class App 
{
    private static final String CONSUMER_KEY = "yourConsumerKey";
    private static final String CONSUMER_SECRET = "yourConsumerSecret";
    private static final String PROTECTED_RESOURCE_ENDPOINT = "https://rest.sandbox-immobilienscout24.de/restapi/api/gis/v1.0/country/276/region";


    public static void main(String[] args) throws IOException, OAuthCommunicationException, OAuthExpectationFailedException, OAuthMessageSignerException {
        // create a consumer object and configure it with the access
        // token and token secret obtained from the service provider
        OAuthConsumer consumer = new DefaultOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);

        // create an HTTP request to a protected resource endpoint
        URL url = new URL(PROTECTED_RESOURCE_ENDPOINT);
        HttpURLConnection request = (HttpURLConnection) url.openConnection();

        // sign the request
        consumer.sign(request);

        // send the request
        request.connect();
    }
}
