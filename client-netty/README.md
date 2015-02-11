## WebPush Client based on Netty
This module contains both a WebPush client and a command line applications called the WebPush Console.

The WebPush Console is intended to be used during development and manual testing of a WebPush server. Currently, it works
with the [node-webpush-server](https://github.com/kitcambridge/node-webpush-server) and the AeroGear
[webpush-server](../server-netty).

## Using the WebPush console
A very basic console/command line application is available to assist in development and testing of the server.

__NOTE__:
The [node-webpush-server](https://github.com/kitcambridge/node-webpush-server) uses the Transport Layer Security (TLS)
extension Next Protocol Negotiation (NPN) and does currently not support Application-Layer Protocol Negotiation (ALPN).
There is no NPN support for Java 1.8 so it is required that Java 1.7 be used to start the WebPush Console. This cannot
be done as a choice at runtime as the jar files for either NPN or ALPN must be added to the Java boot classpath.

### Start the AerogGear server
__Java 1.8__ is required for the AeroGear WebPush Server.

    cd server-netty
    mvn compile exec:exec
    (maven output not displayed)
    [main] INFO org.jboss.aerogear.webpush.netty.WebPushNettyServer - WebPush server bound to localhost:8443

### Start the Node.js server
Follow the [Getting started instructions](https://github.com/kitcambridge/node-webpush-server#getting-started) to start
the Node.js WebPush Server.

    $ webpush-server --port 8080 --key demo.key --cert selfsigned.crt
    Listening on https://Daniels-MBP.lan:8080...

The ```demo.key``` and ```selfsigned.crt``` can be found in [server-netty/src/main/resources](../server-netty/src/main/resources).

### Start the console
__Java 1.8__ is required for the WebPush Console when connecting to the AeroGear WebPush Server, and __Java 1.7__ for
the WebPush Console running against the node-webpush-server.

Press ```tab``` to see the available commands and also to display the available options for specific commands.
Most command also support a ```--help``` option to display information about options that the command accepts.
  
    cd client-netty
    mvn compile exec:exec
    
    [webpush]$

#### Connect

    [webpush]$ connect -h hostname -p port
    Connected to [hostname:port]

__Note__ that when connecting to the ```node-webpush-server``` the host name should be the host printed when upon server
startup. This is because the _node-webpush-server_ supports [Server Name Indication](http://en.wikipedia.org/wiki/Server_Name_Indication).

#### Register 

AeroGear WebPush server:

    [webpush]$ register --path /webpush/register
    > DefaultHttp2Headers[:authority: localhost:8443, :method: POST, :path: /webpush/register, :scheme: https]
    < [streamid:3] DefaultHttp2Headers[:status: 201, access-control-allow-origin: *, access-control-expose-headers: Link, Cache-Control, Location, cache-control: private, max-age=604800000, link: <webpush/aggregate/14752553-f74c-4031-9e0f-1dbc7a54cb45>;rel="urn:ietf:params:push:aggregate", link: <webpush/reg/14752553-f74c-4031-9e0f-1dbc7a54cb45>;rel="urn:ietf:params:push:reg", link: <webpush/subscribe/14752553-f74c-4031-9e0f-1dbc7a54cb45>;rel="urn:ietf:params:push:sub", location: webpush/reg/14752553-f74c-4031-9e0f-1dbc7a54cb45]

Node.js WebPush server:

    [webpush]$ register --path /devices
    > DefaultHttp2Headers[:authority: localhost:7777, :method: POST, :path: /devices, :scheme: https]
    < [streamid:3] DefaultHttp2Headers[:status: 201, cache-control: max-age=3600, private, content-length: 0, date: Tue, 10 Feb 2015 12:37:02 GMT, link: </devices/25b1ac1b-cec7-4ee9-abf7-25e199db6484>; rel="urn:ietf:params:push:reg", </devices/25b1ac1b-cec7-4ee9-abf7-25e199db6484/channels>; rel="urn:ietf:params:push:sub", location: /devices/25b1ac1b-cec7-4ee9-abf7-25e199db6484]

#### Monitor
Use the WebLink of type _urn:ietf:params:push:reg_ from the registration stage above as the url option below:

    [webpush]$ monitor --url url
    > DefaultHttp2Headers[:authority: localhost:8443, :method: GET, :path: webpush/reg/14752553-f74c-4031-9e0f-1dbc7a54cb45, :scheme: https]
    < [streamid:7] DefaultHttp2Headers[:status: 200, access-control-allow-origin: *, access-control-expose-headers: Link, Cache-Control, cache-control: private, max-age=604800000, link: <webpush/aggregate/14752553-f74c-4031-9e0f-1dbc7a54cb45>;rel="urn:ietf:params:push:aggregate", link: <webpush/subscribe/14752553-f74c-4031-9e0f-1dbc7a54cb45>;rel="urn:ietf:params:push:sub"]

#### Create a subscription
Use the WebLink of type _urn:ietf:params:push:sub_ from the registration stage above as the url option below:

    [webpush]$ subscribe --url url
    > DefaultHttp2Headers[:authority: localhost:8443, :method: POST, :path: webpush/subscribe/14752553-f74c-4031-9e0f-1dbc7a54cb45, :scheme: https]
    < [streamid:5] DefaultHttp2Headers[:status: 201, access-control-allow-origin: *, access-control-expose-headers: Location, cache-control: private, max-age=604800000, location: /webpush/zZ9Y1tf1aSjKF135DlJve4TUcbp33tSfiHsalh8a0U%2FTFLd54bCSiVf0KX9YB2jw6W5lVNcBK3aO25C3ccknfpnMO77qJiUitrG4tvKSyhDmIFQFef8ZOCq9RwI1u8H7%2Bg70U0S79gXC]
    

#### Send notification
Use the _location_ header value from the subscription response above as the url below:

    [webpush]$ notify --url url --payload hello
    > DefaultHttp2Headers[:authority: localhost:8443, :method: PUT, :path: /webpush/zZ9Y1tf1aSjKF135DlJve4TUcbp33tSfiHsalh8a0U%2FTFLd54bCSiVf0KX9YB2jw6W5lVNcBK3aO25C3ccknfpnMO77qJiUitrG4tvKSyhDmIFQFef8ZOCq9RwI1u8H7%2Bg70U0S79gXC, :scheme: https]
    < [streamid:2] DefaultHttp2Header]
    < [streamid:2] hello
    [webpush]$ notify --url url --payload hello2
    > DefaultHttp2Headers[:authority: localhost:8443, :method: PUT, :path: /webpush/zZ9Y1tf1aSjKF135DlJve4TUcbp33tSfiHsalh8a0U%2FTFLd54bCSiVf0KX9YB2jw6W5lVNcBK3aO25C3ccknfpnMO77qJiUitrG4tvKSyhDmIFQFef8ZOCq9RwI1u8H7%2Bg70U0S79gXC, :scheme: https]
    < [streamid:2] hello2
    
#### Exit the console
    
    > exit


### WebPush Aggregate Extension
Allows an application to request that a web push server deliver the same message to a potentially large set of devices,
and is a separate specification called [webpush-aggregate](http://tools.ietf.org/html/draft-thomson-webpush-aggregate-00)

This may not be supported by all WebPush server implementation but is supported by the WebPush Console. The following
example is using AeroGear WebPush Server.

    [webpush]$ connect
    Connected to [localhost:8443]

    [webpush]$ register
    > DefaultHttp2Headers[:authority: localhost:8443, :method: POST, :path: /webpush/register, :scheme: https]
    < [streamid:3] DefaultHttp2Headers[:status: 201, access-control-allow-origin: *, access-control-expose-headers: Link, Cache-Control, Location, cache-control: private, max-age=604800000, link: <webpush/aggregate/66a0f93c-1857-4507-a5da-0bb04e26f6b3>;rel="urn:ietf:params:push:aggregate", link: <webpush/reg/66a0f93c-1857-4507-a5da-0bb04e26f6b3>;rel="urn:ietf:params:push:reg", link: <webpush/subscribe/66a0f93c-1857-4507-a5da-0bb04e26f6b3>;rel="urn:ietf:params:push:sub", location: webpush/reg/66a0f93c-1857-4507-a5da-0bb04e26f6b3]

Notice the web link of rel type _urn:ietf:params:push:aggregate_ which is the url that will be used when creating an
aggregate channel.

    [webpush]$ monitor --url webpush/reg/66a0f93c-1857-4507-a5da-0bb04e26f6b3
    > DefaultHttp2Headers[:authority: localhost:8443, :method: GET, :path: webpush/reg/66a0f93c-1857-4507-a5da-0bb04e26f6b3, :scheme: https]
    < [streamid:5] DefaultHttp2Headers[:status: 200, access-control-allow-origin: *, access-control-expose-headers: Link, Cache-Control, cache-control: private, max-age=604800000, link: <webpush/aggregate/66a0f93c-1857-4507-a5da-0bb04e26f6b3>;rel="urn:ietf:params:push:aggregate", link: <webpush/subscribe/66a0f93c-1857-4507-a5da-0bb04e26f6b3>;rel="urn:ietf:params:push:sub"]

Create two subscriptions:

    [webpush]$ subscribe --url webpush/subscribe/66a0f93c-1857-4507-a5da-0bb04e26f6b3
    > DefaultHttp2Headers[:authority: localhost:8443, :method: POST, :path: webpush/subscribe/66a0f93c-1857-4507-a5da-0bb04e26f6b3, :scheme: https]
    < [streamid:7] DefaultHttp2Headers[:status: 201, access-control-allow-origin: *, access-control-expose-headers: Location, cache-control: private, max-age=604800000, location: /webpush/vlual30OKuEN4OPoXtj73Dsys%2FG05GJXe%2BK1%2FQDFJhcMfbCr8XZF5W5axRu5whKr%2BktqaUGIoMqMRfQPlSr%2Bq0wO2APBtvvdX4%2FDFJbT3wiLC5ug16BzP2%2B1zyyluv2ujVzXinrLeCkt]

    [webpush]$ subscribe --url webpush/subscribe/66a0f93c-1857-4507-a5da-0bb04e26f6b3
    > DefaultHttp2Headers[:authority: localhost:8443, :method: POST, :path: webpush/subscribe/66a0f93c-1857-4507-a5da-0bb04e26f6b3, :scheme: https]
    < [streamid:9] DefaultHttp2Headers[:status: 201, access-control-allow-origin: *, access-control-expose-headers: Location, cache-control: private, max-age=604800000, location: /webpush/m2avCgWq14%2FeuuAQiGxZr%2BmmMZJp88FBQ%2BV42irZTm62ORyhg85IWuffZOdoEnkZx3wTs2St4IIaJxy%2FrJfq5eZkAsj355hDdARXrncTbiutv5rgcGGzG2GzDCeZaeHS6KgS9WAAvf5q]

Next, create the aggregate channel using the web link of type _urn:ietf:params:push:aggregate_ from the above _register_ command.
This command will also print the JSON format of the request body.

    [webpush]$ aggregate --url webpush/aggregate/66a0f93c-1857-4507-a5da-0bb04e26f6b3 --channels /webpush/vlual30OKuEN4OPoXtj73Dsys%2FG05GJXe%2BK1%2FQDFJhcMfbCr8XZF5W5axRu5whKr%2BktqaUGIoMqMRfQPlSr%2Bq0wO2APBtvvdX4%2FDFJbT3wiLC5ug16BzP2%2B1zyyluv2ujVzXinrLeCkt,/webpush/m2avCgWq14%2FeuuAQiGxZr%2BmmMZJp88FBQ%2BV42irZTm62ORyhg85IWuffZOdoEnkZx3wTs2St4IIaJxy%2FrJfq5eZkAsj355hDdARXrncTbiutv5rgcGGzG2GzDCeZaeHS6KgS9WAAvf5q
    > DefaultHttp2Headers[:authority: localhost:8443, :method: POST, :path: webpush/aggregate/66a0f93c-1857-4507-a5da-0bb04e26f6b3, :scheme: https]
    [ {
      "/webpush/m2avCgWq14%2FeuuAQiGxZr%2BmmMZJp88FBQ%2BV42irZTm62ORyhg85IWuffZOdoEnkZx3wTs2St4IIaJxy%2FrJfq5eZkAsj355hDdARXrncTbiutv5rgcGGzG2GzDCeZaeHS6KgS9WAAvf5q" : {
        "expires" : 0
      }
    }, {
      "/webpush/vlual30OKuEN4OPoXtj73Dsys%2FG05GJXe%2BK1%2FQDFJhcMfbCr8XZF5W5axRu5whKr%2BktqaUGIoMqMRfQPlSr%2Bq0wO2APBtvvdX4%2FDFJbT3wiLC5ug16BzP2%2B1zyyluv2ujVzXinrLeCkt" : {
        "expires" : 0
      }
    } ]
    < [streamid:11] DefaultHttp2Headers[:status: 201, access-control-allow-origin: *, access-control-expose-headers: Location, cache-control: private, max-age=604800000, location: /webpush/%2FgVm34r0gfnFphFCxxRPSodg94d4zH7CWWd76t0%2BVsMiNIZ%2F1Or%2B52w9X6k913dgHIK0XJnLg7bOsAzLPdvR7RWBzx4mqTTFTWzyymw26J7RCMR76CdNvdfqY%2FnG2s7KtBVVtzijw54p]

Finally, we can use the _notify_ command which is the same as before only the url for the aggreate channel, which is
the value of the _location_ response header from the _aggregate_ command above:

    [webpush]$ notify --url /webpush/%2FgVm34r0gfnFphFCxxRPSodg94d4zH7CWWd76t0%2BVsMiNIZ%2F1Or%2B52w9X6k913dgHIK0XJnLg7bOsAzLPdvR7RWBzx4mqTTFTWzyymw26J7RCMR76CdNvdfqY%2FnG2s7KtBVVtzijw54p --payload hello
    > DefaultHttp2Headers[:authority: localhost:8443, :method: PUT, :path: /webpush/%2FgVm34r0gfnFphFCxxRPSodg94d4zH7CWWd76t0%2BVsMiNIZ%2F1Or%2B52w9X6k913dgHIK0XJnLg7bOsAzLPdvR7RWBzx4mqTTFTWzyymw26J7RCMR76CdNvdfqY%2FnG2s7KtBVVtzijw54p, :scheme: https]
    < [streamid:2] hello
    < [streamid:2] hello




