### Introduction
This is a spring boot application, which executes the three-legged OAuth1.0a flow and communicates with the ImmoScout24 API.
We use Spring Security to secure the application with a login dialog and to sign requests to the ImmoScout24 API.

### Running the example application
1. First you need to set your own CLIENT_KEY and CLIENT_SECRET in the code.
2. Run the application with maven using `mvn spring-boot:run`.
3. Once the application is running, Spring will print a password to stdin which you can use to login to the application.
  The password will be shown in the logs as `Using generated security password: <spring security password>`
4. Open a browser and navigate to: http://localhost:8080/load-real-estates
5. Log in with the credentials:
    * username: user
    * password: *<Spring security generated password>*
6. The application will initiate the three-legged OAuth1.0a flow in which you will be redirected to ImmoScout24,
    where you need to confirm that the application can communicate with the ImmoScout24 API.
    ImmoScout24 redirects you back to the application where you will then see real-estate data that has been requested from the ImmoScout24 API.
