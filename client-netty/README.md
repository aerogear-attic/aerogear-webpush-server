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
    [nioEventLoopGroup-0-0] INFO org.jboss.aerogear.webpush.WebPushClientInitializer -
    ----------------OUTBOUND--------------------
    SETTINGS: ack=false, settings={HEADER_TABLE_SIZE=4096, ENABLE_PUSH=1, MAX_CONCURRENT_STREAMS=2147483647, INITIAL_WINDOW_SIZE=65535, MAX_FRAME_SIZE=16384, MAX_HEADER_LIST_SIZE=2147483647}
    ------------------------------------
    [nioEventLoopGroup-0-0] DEBUG io.netty.handler.ssl.util.InsecureTrustManagerFactory - Accepting a server certificate: CN=example.com
    [nioEventLoopGroup-0-0] DEBUG io.netty.handler.ssl.SslHandler - [id: 0x957cfe32, /127.0.0.1:57119 => localhost/127.0.0.1:8443] HANDSHAKEN: TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
    [nioEventLoopGroup-0-0] INFO org.jboss.aerogear.webpush.WebPushClientInitializer -
    ----------------INBOUND--------------------
    SETTINGS: ack=false, settings={HEADER_TABLE_SIZE=4096, MAX_CONCURRENT_STREAMS=2147483647, INITIAL_WINDOW_SIZE=65535, MAX_FRAME_SIZE=16384, MAX_HEADER_LIST_SIZE=2147483647}
    ------------------------------------
    [nioEventLoopGroup-0-0] INFO org.jboss.aerogear.webpush.WebPushClientInitializer -
    ----------------OUTBOUND--------------------
    SETTINGS ack=true
    ------------------------------------
    [nioEventLoopGroup-0-0] INFO org.jboss.aerogear.webpush.WebPushClientInitializer -
    ----------------INBOUND--------------------
    SETTINGS ack=true
    ------------------------------------
    
#### Register 
    > register
    nioEventLoopGroup-0-0] INFO org.jboss.aerogear.webpush.WebPushClientInitializer -
    ----------------OUTBOUND--------------------
    HEADERS: streamId:3, headers=DefaultHttp2Headers[:method: POST, :path: webpush/register, :scheme: localhost], streamDependency=0, weight=16, exclusive=false, padding=0, endStream=true
    ------------------------------------
    > [nioEventLoopGroup-0-0] INFO org.jboss.aerogear.webpush.WebPushClientInitializer -
    ----------------INBOUND--------------------
    HEADERS: streamId:3, headers=DefaultHttp2Headers[:status: 200, cache-control: private, max-age=604800000, link: <webpush/4336b0de-c848-41a5-9406-8cf1e3390dbd/channel>;rel="push:channel", location: webpush/4336b0de-c848-41a5-9406-8cf1e3390dbd/monitor], streamDependency=0, weight=16, exclusive=false, padding=0, endStream=false
    ------------------------------------
    
#### Create a channel
    
    > channel webpush/4336b0de-c848-41a5-9406-8cf1e3390dbd/channel
    [nioEventLoopGroup-0-0] INFO org.jboss.aerogear.webpush.WebPushClientInitializer -
    ----------------OUTBOUND--------------------
    HEADERS: streamId:5, headers=DefaultHttp2Headers[:method: POST, :path: webpush/4336b0de-c848-41a5-9406-8cf1e3390dbd/channel, :scheme: localhost], streamDependency=0, weight=16, exclusive=false, padding=0, endStream=true
    ------------------------------------
    > [nioEventLoopGroup-0-0] INFO org.jboss.aerogear.webpush.WebPushClientInitializer -
    ----------------INBOUND--------------------
    HEADERS: streamId:5, headers=DefaultHttp2Headers[:status: 201, cache-control: private, max-age=604800000, location: %2F0JH0kbm31Zoz6%2BHpApg2SJ7yOXXz3V7sLPi93CpeZoqZKPfCfpVJWGkQi%2FIGDYPuncfdFuNQ6RiZOy07Tmwp8vp5bWwUw18iIiUoV3VlRWjzFC%2B6AV2IYicfyvCfx%2BKpbUJlSfM82eE], streamDependency=0, weight=16, exclusive=false, padding=0, endStream=false
    ------------------------------------
    
#### Monitor    
    > monitor webpush/4336b0de-c848-41a5-9406-8cf1e3390dbd/monitor
    [nioEventLoopGroup-0-0] INFO org.jboss.aerogear.webpush.WebPushClientInitializer -
    ----------------OUTBOUND--------------------
    HEADERS: streamId:7, headers=DefaultHttp2Headers[:method: GET, :path: webpush/4336b0de-c848-41a5-9406-8cf1e3390dbd/monitor, :scheme: localhost], streamDependency=0, weight=16, exclusive=false, padding=0, endStream=true
    ------------------------------------
    > [nioEventLoopGroup-0-0] INFO org.jboss.aerogear.webpush.WebPushClientInitializer -
    ----------------INBOUND--------------------
    PUSH_PROMISE: streamId=7, promisedStreamId=2, headers=DefaultHttp2Header], padding=0
    ------------------------------------
    [nioEventLoopGroup-0-0] INFO org.jboss.aerogear.webpush.WebPushClientInitializer -
    ----------------OUTBOUND--------------------
    GO_AWAY: lastStreamId=2, errorCode=1, length=75, bytes=556e7265636f676e697a656420485454502073746174757320636f646520276e756c6c2720656e636f756e746572656420696e207472616e736c6174696f6e20746f20485454502f312e78
    ------------------------------------
    
#### Exit the console
    
    > quit
    
This is really as far as I've gotten. I need to figure out how push promises should be handled on both the server and
the client side. 
    



