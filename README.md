# Aerogear WebPush Server 
This project is a proof of concept implementation of the 
[WebPush Protocol](http://tools.ietf.org/html/draft-thomson-webpush-http2-02) specification with the purpose of gaining
a better understanding of the specification.

IETF [charter](https://datatracker.ietf.org/wg/webpush/charter)


This [screen cast](https://drive.google.com/file/d/0B2E1HZ1JnrJfYW90eVBTaGkzSkU/view?usp=sharing) shows a sample 
interaction of the server and client and might help explain things.


One thing to keep in mind when reading [draft-thomson-webpush-http2-02](https://tools.ietf.org/html/draft-thomson-webpush-http2-02) 
is that we are talking about a single communication channel for all applications using HTTP/2.   

This differs from SimplePush where we have a separate communication channel (perhaps WebSocket) per application. Each 
application might have multiple SimplePush channels but if you have two separate applications they would not be sharing 
the same WebSocket. 
With this proposal a web application would use the [push-api](https://w3c.github.io/push-api/index.html)  which uses 
a service worker that manages the push stuff. You can think of this as it is the User Agent that the WebPush Server 
communicates with as opposed to directly to the web application in the case of SimplePush.
With this setup it is possible to have a single HTTP/2 connection to the WebPush server. 

## Registration
A device/client (consumer of Push messages) registers with the WebPush server. This established a persistent HTTP/2 
connection between the device/client and the WebPush Server.  

The response of a registration contains information about how the device can create new channels and how expected to 
monitor for push messages.   

For a webapp it might be given that it would use the existing HTTP/2 connection, but for a mobil device it might make 
more sense to use a different communication channel. For example, with Google it might make sense to use 
Google Cloud Messaging. Similar for iOS perhaps but there Apple Push Notification Service could be used.  __This is 
something that we need to look into how this could work__

There are two HTTP/2 headers, ```location``` and ```link```, that will be returned to a registration request which are described below.

### Location header
This header contains the URL which the device can use to monitor. 

    location: <webpush/123/monitor>;rel="push:monitor"

A device can issue a GET request against the above URl to monitor a resource. This allows the server to reserv a 
stream by writing a push promise in response to this GET request. This allows the server to have a HTTP/2 stream
on which it can push notifications to the device.  

It is possible that a device first registers, then creates a number of channels, and at a later time calls monitor. 
During this time push notifications can arrive at the server and stored. When a device sends the monitor request it can
specify that a ```Prefer: wait=0```, which will cause the server to send all the stored messages straight away.

### Link header
This contains a [WebLink](https://tools.ietf.org/html/rfc5988) with a two entries:

    link: <webpush/123/channel>;rel="push:channel,<webpush/123/monitor>;rel="push:monitor"

A device can use the above ```link``` with the ```push:channel``` type to create new channels. The ```push:monitor```
link identical to the one returned in the ```location``` header.
    
_Note_: the link types ```push:channel``` and ```push:monitor``` are currently specified as ```...:push:monitor``` and
```...:push:channel``` in the specification. This is simply to indicate that they are not defined/registered URNs yet.

## Channel creation
A device can use the ```link``` header type of ```push:channel``` to create new channels. This is done
by issueing a POST request.   
The response status code will be ```201``` indicating that the channel has been created
and the ```location``` header will include the URL to the channel. This URL is what a backend server uses when it 
wants to send notifications.

Channels can expire and this is indicated by the ```max-age``` parameter on the ```cache-control``` header field.


## Channel deletion
This can be done by sending a DELETE to the channel URL.

    
## Prerequisites 
* This project requires Java 8.

* This project depends on [Netty](http://netty.io/)'s HTTP/2 support which is currently scheduled for Netty 5. 
This means that a local bulid of Netty's [master](https://github.com/netty/netty) branch is required to build 
and run this project.


AeroGear WebPush consists of the following modules:

* [common](./common)  
Just common classes used by multiple modules in the project.

* [datastores](./datastores)  
Contains implementations of various datastores. Please see the specific datastore's readme for further details.

* [protocol](./protocol)  
The WebPush Server Protocol provides interfaces for the protocol.

* [server-api](./server-api)  
An API for AeroGear WebPush Server

* [server-core](./server-core)  
An implementation of AeroGear WebPush Server API.

* [server-netty](./server-netty)  
The WebPush Server implementation that uses Netty 5.x.

* [client-netty](./client-netty)  
The WebPush Client implementation that uses Netty 5.x.

Please refer to the above modules documentation for more information.
