### Introduction
This documentation describes how to run the code example to make a 2-legged-oauth1-request.

## Code insight
- The application creates an HTTP request,
 which will be signed from a Java library called "SignPost" in order to be able do the authentication in the service provider.

- The method <code>printResponse()</code> prints out the response which you will get from the service provider.

## Steps to do before running the code
* Change the Consumer Key and Consumer Secret (Code line: 17-18) to your  own key and secret.
* Run the code (Right click -> run TwoLeggedOAuth1FlowApplication), or through maven:
1) run <code>mvn compile</code>
2) run  <code>mvn exec:java -Dexec.mainClass=de.is24.TwoLeggedOAuth1FlowApplication</code>

## How should response look like ?
- The response will be in XML format.
- The information depend on the Api endpoint which you are sending your request to. 

**Note:** If you don't change the consumer key and secret in lines 17-18, the response will look like following:
401
_**XML schema:**_
<div id="code">
```
<common:messages xmlns:common="http://rest.immobilienscout24.de/schema/common/1.0">
    <message>
        <messageCode>ERROR_AUTHENTICATION_REQUIRED</messageCode>
        <message>Consumer not found: yourConsumerKey</message>
    </message>
</common:messages>
```
</div>