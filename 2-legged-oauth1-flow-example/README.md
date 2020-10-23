### Introduction
This documentation contains example code which makes a two-legged OAuth1.0a request to the ImmoScout24 API using the Signpost Java library.

## Running the example application
1. Change the `CONSUMER_KEY` and `CONSUMER_SECRET` to your own key and secret.
2. run <code>mvn compile</code>
3. run <code>mvn exec:java -Dexec.mainClass=de.is24.TwoLeggedOAuth1FlowApplication</code>
4. The code will perform an API request against the region search API and print out the response.
