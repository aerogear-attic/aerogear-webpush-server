## WebPush Client based on Netty


## Using the WebPush console
A very basic console/command line application is available to assist in development and testing of the server. 
 
### Start the server

    cd server-netty
    mvn exec:exec
    
    ...
    main] INFO org.jboss.aerogear.webpush.netty.WebPushNettyServer - WebPush server bound to localhost:8443
    
### Start the console
  
    cd client-netty
    mvn exec:exec
    
    WebPush console
    > 

#### Connect
    WebPush console
    > connect localhost:8443
    Connected to [localhost:8443]
    >
    
#### Register 
    > register
    > Headers: DefaultHttp2Headers[:status: 200, cache-control: private, max-age=604800000, link: <webpush/531cc9eb-7978-487b-b517-1a56a4291cb2/channel>;rel="push:channel", location: webpush/531cc9eb-7978-487b-b517-1a56a4291cb2/monitor]
    >
    
#### Create a channel
    
    > channel webpush/531cc9eb-7978-487b-b517-1a56a4291cb2/channel
    > Headers: DefaultHttp2Headers[:status: 201, cache-control: private, max-age=604800000, location: webpush/P8E9BM32X%2BEQZDvltW%2BhuGeuGmQeQSxpqewRB7Awb9UIAWFxK%2BvIovuTwkMI8xocwwW2sStHfzg0OEC9Ovo41FDCz5nYXHSgFDOKYANsFwX1moavATBX0cruZkc1ySTVSngjpoMwvx8x]
    >
    
#### Monitor    
    > monitor webpush/531cc9eb-7978-487b-b517-1a56a4291cb2/monitor
    >
    
#### Send notification
    > monitor webpush/531cc9eb-7978-487b-b517-1a56a4291cb2/monitor
    > notify webpush/P8E9BM32X%2BEQZDvltW%2BhuGeuGmQeQSxpqewRB7Awb9UIAWFxK%2BvIovuTwkMI8xocwwW2sStHfzg0OEC9Ovo41FDCz5nYXHSgFDOKYANsFwX1moavATBX0cruZkc1ySTVSngjpoMwvx8x hello
    > Headers: DefaultHttp2Header]
    > Got notification: hello
    
    > notify webpush/P8E9BM32X%2BEQZDvltW%2BhuGeuGmQeQSxpqewRB7Awb9UIAWFxK%2BvIovuTwkMI8xocwwW2sStHfzg0OEC9Ovo41FDCz5nYXHSgFDOKYANsFwX1moavATBX0cruZkc1ySTVSngjpoMwvx8x helloWorld
    > Got notification: helloWorld
    >
    
#### Exit the console
    
    > quit
    
This is really as far as I've gotten. I need to figure out how push promises should be handled on both the server and
the client side. 
    



