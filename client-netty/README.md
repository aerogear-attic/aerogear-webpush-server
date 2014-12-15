## WebPush Client based on Netty


## Using the WebPush console
A very basic console/command line application is available to assist in development and testing of the server. 

### Start the server

    cd server-netty
    mvn exec:exec
    (maven output not displayed)
    [main] INFO org.jboss.aerogear.webpush.netty.WebPushNettyServer - WebPush server bound to localhost:8443
    
### Start the console
The console support tab completion and you can type ```help``` to see the available commands.
  
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
    < [streamid:3] DefaultHttp2Headers[:status: 200, access-control-allow-origin: *, access-control-expose-headers: Link, Cache-Control, Location, cache-control: private, max-age=604800000, link: <webpush/97e360b2-383d-4de5-bf28-8956977599e9/aggregate>;rel="urn:ietf:params:push:aggregate", link: <webpush/97e360b2-383d-4de5-bf28-8956977599e9/reg>;rel="urn:ietf:params:push:reg", link: <webpush/97e360b2-383d-4de5-bf28-8956977599e9/subscribe>;rel="urn:ietf:params:push:sub", location: webpush/97e360b2-383d-4de5-bf28-8956977599e9/reg]
    >
    
#### Create a subscription
    
    > subscribe webpush/97e360b2-383d-4de5-bf28-8956977599e9/subscribe
    < [streamid:5] DefaultHttp2Headers[:status: 201, access-control-allow-origin: *, access-control-expose-headers: Location, cache-control: private, max-age=604800000, location: /webpush/Lc7OqncNa7b0v%2FRAoOp8sA0Z3t74IAN0RGHQ%2Bf%2BxLRWOTwnmDjOuzlWmSOvHqD93s%2B5Nm8lx7AeCheRKfxxqc%2BpSLfE79xymKiDwtTBgaIHRQNeD5e6WBSIvWJEzDndou3l7OLZ5lCkF]
    >
    
#### Monitor    

    > monitor webpush/97e360b2-383d-4de5-bf28-8956977599e9/reg
    < [streamid:7] DefaultHttp2Headers[:status: 200, access-control-allow-origin: *, access-control-expose-headers: Link, Cache-Control, cache-control: private, max-age=604800000, link: <webpush/97e360b2-383d-4de5-bf28-8956977599e9/aggregate>;rel="urn:ietf:params:push:aggregate", link: <webpush/97e360b2-383d-4de5-bf28-8956977599e9/subscribe>;rel="urn:ietf:params:push:sub"]
    
#### Send notification

    > notify /webpush/Lc7OqncNa7b0v%2FRAoOp8sA0Z3t74IAN0RGHQ%2Bf%2BxLRWOTwnmDjOuzlWmSOvHqD93s%2B5Nm8lx7AeCheRKfxxqc%2BpSLfE79xymKiDwtTBgaIHRQNeD5e6WBSIvWJEzDndou3l7OLZ5lCkF hello
    < [streamid:2] hello
    >
    
#### Exit the console
    
    > quit
    



