### Introduction
- This is a spring boot application, which simulates a 3-legged-OAuth1 process.
- The documentation describes the flow of 3-legged oauth1,
 in order to have an access token, which with it you will be able to get data from a protected resource.
 
 
 ### Spring application insight
 - The application is using spring security, which uses by default a client credentials login security layer.
 
 
 ### Steps to run the application locally
 - First thing you need to do is to set your own CLIENT_KEY and CLIENT_SECRET in code lines 34-35.
 - When the application runs, Spring will create a password to use it into the login page which Spring security creates.
    ** the password will be shown in the logs as _*Using generated security password: 6865a337-3a2e-4cce-9531-4f82a3f76d41*_
 - In the browser, navigate to: http://localhost:8080
 - Insert following credentials:
    * username: user
    * password: *Spring security generated password*
    
 - Then you will be redirected to the ACCESS_CONFIRMATION_URL, where you need to confirm that the Consumer (Application) can (in the name of you) communicate with the service provider (Immoscout).
 - Then an access token will be obtained and with it you get the data from you need from the protected resource.
 
 
 ### What happens in code ? 
 - A Protected data resource will be simulated with specific configuration.
 - After user logs in with spring security credentials, the method <code>initializeTokenExchange()</code> will be called, which into it a request token will be gained.
 - The user will be redirected to a specific ACCESS_CONFIRMATION_URL to confirm.
 - After confirming, the method <code>oauthCallback()</code> where into it the request token will be exchanged into an access token.
 - With the access token the user will be redirected to the targeted api to get data from the protected data resource, as you can see in the method <code>loadRealEstates()</code> (code line : 105-110)