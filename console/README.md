## WebPush Console
This module contains WebPush client command line applications called the WebPush Console.

The WebPush Console is intended to be used during development and manual testing of a WebPush server.

## Using the WebPush console
A very basic console/command line application is available to assist in development and testing of the server.

### Start the AerogGear server
__Java 1.8__ is required for the AeroGear WebPush Server.

    cd server-netty
    mvn compile exec:exec
    (maven output not displayed)
    [main] INFO org.jboss.aerogear.webpush.netty.WebPushNettyServer - WebPush server bound to localhost:8443

The ```demo.key``` and ```selfsigned.crt``` can be found in [server-netty/src/main/resources](../server-netty/src/main/resources).

### Start the console
__Java 1.8__ is required for the WebPush Console when connecting to the AeroGear WebPush Server.


Press ```tab``` to see the available commands and also to display the available options for specific commands.
Most command also support a ```--help``` option to display information about options that the command accepts.
  
    cd console
    mvn compile exec:exec
    
    [webpush]$

#### Connecting to the Server

    [webpush]$ connect -h hostname -p port
    Connected to [hostname:port]

#### Subscribing for Push Messages

    [webpush]$ subscribe --url /webpush/subscribe
    > DefaultHttp2Headers[:authority: localhost:8443, :method: POST, :path: /webpush/subscribe, :scheme: https]
    < [streamId:3] DefaultHttp2Headers[:status: 201, access-control-allow-origin: *, access-control-expose-headers: link, cache-control, location, cache-control: private, max-age=604800000, link: </webpush/p/MJzDNtVKd9KO0XclJfDXYzhOh3sx%2BhePggdyfWtG%2BjAUb74O6DhsPDSlNkrFnspAj75DzDKWYyYEDKt%2BiGwfk%2FgAMrJD3GGxAH1l5xyYoInks6B90GGbIEE77HDF%2FM9cXzKZdWxcgpPB>;rel="urn:ietf:params:push", link: </webpush/receipts/u9HAZeFzbUvzkiDOhxOHb%2BLtPZtSyp47tL9w5ZSDY%2FYA0%2BJOfjV6sH%2F4woxz4zIKOTTxB8MX%2Fwe2qNHwsSKKok8mVCU%3D>;rel="urn:ietf:params:push:receipt", location: /webpush/s/bc949630-341a-44d0-8d11-6bd160588f8d]

#### Receiving Push Messages
Use the _location_ header value from the subscription response above as the url below:

    [webpush]$ monitor --url /webpush/s/bc949630-341a-44d0-8d11-6bd160588f8d
    > DefaultHttp2Headers[:authority: localhost:8443, :method: GET, :path: /webpush/s/bc949630-341a-44d0-8d11-6bd160588f8d, :scheme: https]

You can also use _--nowait_ option, if you want only check availability of pending Push Messages and do not monitor new ones.
A 204 (No Content) status code with no associated server pushes indicates that no messages are presently available.

    [webpush]$ monitor --url /webpush/s/bc949630-341a-44d0-8d11-6bd160588f8d --nowait 
    > DefaultHttp2Headers[:authority: localhost:8443, :method: GET, :path: /webpush/s/bc949630-341a-44d0-8d11-6bd160588f8d, :scheme: https, prefer: wait=0]
    < [streamId:5] DefaultHttp2Headers[:status: 204, access-control-allow-origin: *]

#### Requesting Push Message Delivery
Use the WebLink of type _urn:ietf:params:push_ from the subscription response above as the url below:

    [webpush]$ notify --url /webpush/p/MJzDNtVKd9KO0XclJfDXYzhOh3sx%2BhePggdyfWtG%2BjAUb74O6DhsPDSlNkrFnspAj75DzDKWYyYEDKt%2BiGwfk%2FgAMrJD3GGxAH1l5xyYoInks6B90GGbIEE77HDF%2FM9cXzKZdWxcgpPB --payload hello
    > DefaultHttp2Headers[:authority: localhost:8443, :method: POST, :path: /webpush/p/MJzDNtVKd9KO0XclJfDXYzhOh3sx%2BhePggdyfWtG%2BjAUb74O6DhsPDSlNkrFnspAj75DzDKWYyYEDKt%2BiGwfk%2FgAMrJD3GGxAH1l5xyYoInks6B90GGbIEE77HDF%2FM9cXzKZdWxcgpPB, :scheme: https]
    < [streamId:9] DefaultHttp2Headers[:status: 201, access-control-allow-origin: *, access-control-expose-headers: location, location: /webpush/d/qOyBjcw5ilSJM5uTviPBPhP%2BRpUKMgrILfb2w2%2Bd15JcfT%2BADUL1RIrACThAiwstDf6wYNoWWaawUVkfaoA3Q3ABx6OzmetNynhSJlqA7gnLneqhpK2hYmrGdsRWfFuN6POLB9B3ySDs]

User Agent, which monitor its own subscription, will receive the Push Message via HTTP/2 Server Push:

    < [streamId:7, promisedStreamId:2] DefaultHttp2Headers[:authority: localhost:8443, :method: GET, :path: /webpush/d/fDeM5XViE98VT%2B1WfdMEBfjMIuGCwJzmQAirLPFY0XFwRsE%2FDYfww2%2Flj7fu%2Fed4UKRETQyqvJndTIGfX6u%2BUtRFtFbeH2q4nwTpfszX9mOZg1xq1chKPbi5IPkZSut5V8FvYGTRBxcL]
    < [streamId:2] DefaultHttp2Headers[:status: 200, access-control-allow-origin: *, access-control-expose-headers: cache-control, content-type, cache-control: private, max-age=604800000, content-length: 5, content-type: text/plain;charset=utf8]
    < [streamId:2] hello

#### Subscribing for Push Message Receipts
Use the WebLink of type _urn:ietf:params:push:receipt_ from the subscription response above as the url below:

    [webpush]$ receipt --url /webpush/receipts/u9HAZeFzbUvzkiDOhxOHb%2BLtPZtSyp47tL9w5ZSDY%2FYA0%2BJOfjV6sH%2F4woxz4zIKOTTxB8MX%2Fwe2qNHwsSKKok8mVCU%3D
    > DefaultHttp2Headers[:authority: localhost:8443, :method: POST, :path: /webpush/receipts/u9HAZeFzbUvzkiDOhxOHb%2BLtPZtSyp47tL9w5ZSDY%2FYA0%2BJOfjV6sH%2F4woxz4zIKOTTxB8MX%2Fwe2qNHwsSKKok8mVCU%3D, :scheme: https]
    < [streamId:11] DefaultHttp2Headers[:status: 201, access-control-allow-origin: *, access-control-expose-headers: location, location: /webpush/r/s5dcKdO03yK6vBFiwcagkKGkzHVJ8TBs4PJHL0ATVo%2BuYkB5nqDRI%2FYwCqI0eoZ6xvlfaJeb%2BssUqwTgMeBZ%2FE5RMpyLieTY1vG8SVR79SUJtJnDQRQVVsIAoUOxOKkI86pKnmEvtpWz]

#### Receiving Push Message Receipts
Use the _location_ header value from the receipt response above as the url below:

    [webpush]$ acks --url /webpush/r/s5dcKdO03yK6vBFiwcagkKGkzHVJ8TBs4PJHL0ATVo%2BuYkB5nqDRI%2FYwCqI0eoZ6xvlfaJeb%2BssUqwTgMeBZ%2FE5RMpyLieTY1vG8SVR79SUJtJnDQRQVVsIAoUOxOKkI86pKnmEvtpWz
    > DefaultHttp2Headers[:authority: localhost:8443, :method: GET, :path: /webpush/r/s5dcKdO03yK6vBFiwcagkKGkzHVJ8TBs4PJHL0ATVo%2BuYkB5nqDRI%2FYwCqI0eoZ6xvlfaJeb%2BssUqwTgMeBZ%2FE5RMpyLieTY1vG8SVR79SUJtJnDQRQVVsIAoUOxOKkI86pKnmEvtpWz, :scheme: https]

Now you are able to send a new Push Message with request for Push Message Receipt:

    [webpush]$ notify --receiptUrl /webpush/r/s5dcKdO03yK6vBFiwcagkKGkzHVJ8TBs4PJHL0ATVo%2BuYkB5nqDRI%2FYwCqI0eoZ6xvlfaJeb%2BssUqwTgMeBZ%2FE5RMpyLieTY1vG8SVR79SUJtJnDQRQVVsIAoUOxOKkI86pKnmEvtpWz --url /webpush/p/MJzDNtVKd9KO0XclJfDXYzhOh3sx%2BhePggdyfWtG%2BjAUb74O6DhsPDSlNkrFnspAj75DzDKWYyYEDKt%2BiGwfk%2FgAMrJD3GGxAH1l5xyYoInks6B90GGbIEE77HDF%2FM9cXzKZdWxcgpPB --payload hello2
    > DefaultHttp2Headers[:authority: localhost:8443, :method: POST, :path: /webpush/p/MJzDNtVKd9KO0XclJfDXYzhOh3sx%2BhePggdyfWtG%2BjAUb74O6DhsPDSlNkrFnspAj75DzDKWYyYEDKt%2BiGwfk%2FgAMrJD3GGxAH1l5xyYoInks6B90GGbIEE77HDF%2FM9cXzKZdWxcgpPB, :scheme: https, push-receipt: /webpush/r/s5dcKdO03yK6vBFiwcagkKGkzHVJ8TBs4PJHL0ATVo%2BuYkB5nqDRI%2FYwCqI0eoZ6xvlfaJeb%2BssUqwTgMeBZ%2FE5RMpyLieTY1vG8SVR79SUJtJnDQRQVVsIAoUOxOKkI86pKnmEvtpWz]
    < [streamId:15] DefaultHttp2Headers[:status: 201, access-control-allow-origin: *, access-control-expose-headers: location, location: /webpush/d/0HsGZycMBV3XAjbLkpIm4CdRLJAfS5q%2BnyDUWOKmKn1jluZ0kP9qkCadxxcDhYfnP8u%2BDiIl9B7A7DSAKwoh%2BIVbpLZUr0g914pGyygon%2BB87TeErs4FUicOhGc19MVI8p%2Fen2sKmPWw]

User Agent, which monitor its own subscription, will receive the Push Message via HTTP/2 Server Push:

    < [streamId:7, promisedStreamId:4] DefaultHttp2Headers[:authority: localhost:8443, :method: GET, :path: /webpush/d/wZf%2FoE0OcYY%2BRzdjvIkXJ2ZRVgmS%2FkJxu2LlZ30nj%2Bct8lSOeMbeAaeSBkl0qwE2mk2kHYs6GweIub02tUhdD6arBbe8bI6H5W9J6YLZigzqYSOt0PY1vEbIwziK%2Fj25uAlA2gag93sG]
    < [streamId:4] DefaultHttp2Headers[:status: 200, access-control-allow-origin: *, access-control-expose-headers: cache-control, content-type, cache-control: private, max-age=604800000, content-length: 6, content-type: text/plain;charset=utf8]
    < [streamId:4] hello2

#### Acknowledging Push Messages
User Agent acknowledge receipt of the message. Use _path_ value from the PUSH_PROMISE frame above as the url below:

    [webpush]$ ack --url /webpush/d/wZf%2FoE0OcYY%2BRzdjvIkXJ2ZRVgmS%2FkJxu2LlZ30nj%2Bct8lSOeMbeAaeSBkl0qwE2mk2kHYs6GweIub02tUhdD6arBbe8bI6H5W9J6YLZigzqYSOt0PY1vEbIwziK%2Fj25uAlA2gag93sG
    > DefaultHttp2Headers[:authority: localhost:8443, :method: DELETE, :path: /webpush/d/wZf%2FoE0OcYY%2BRzdjvIkXJ2ZRVgmS%2FkJxu2LlZ30nj%2Bct8lSOeMbeAaeSBkl0qwE2mk2kHYs6GweIub02tUhdD6arBbe8bI6H5W9J6YLZigzqYSOt0PY1vEbIwziK%2Fj25uAlA2gag93sG, :scheme: https]

Push Server pushes a delivery receipt to the application server.
A 410 (Gone) status code confirms that the message was delivered and acknowledged.

    < [streamId:13, promisedStreamId:6] DefaultHttp2Headers[:authority: localhost:8443, :method: GET, :path: /webpush/d/WEP7Fhc9WCsLqdcAqofLCTMqZcEsuUOcBVmGz2StiPOJBMJ%2BKZRgMw%2FHk3NenAruq5qSt2Mhj%2FJ2tMVPTrfV4CGxOdaquMphQlBXjDRvKkQXc5nJ%2B53BDZDF%2FeUD1g26o1dUcJ8zJafI]
    < [streamId:6] DefaultHttp2Headers[:status: 410, access-control-allow-origin: *]

#### Delete Subscription
Use the _location_ header value from the subscription response above as the url below:

    [webpush]$ delete --url /webpush/s/bc949630-341a-44d0-8d11-6bd160588f8d
    > DefaultHttp2Headers[:authority: localhost:8443, :method: DELETE, :path: /webpush/s/bc949630-341a-44d0-8d11-6bd160588f8d, :scheme: https]

#### Delete Receipt Subscription
Use the _location_ header value from the receipt response above as the url below:

    [webpush]$ delete --url /webpush/r/s5dcKdO03yK6vBFiwcagkKGkzHVJ8TBs4PJHL0ATVo%2BuYkB5nqDRI%2FYwCqI0eoZ6xvlfaJeb%2BssUqwTgMeBZ%2FE5RMpyLieTY1vG8SVR79SUJtJnDQRQVVsIAoUOxOKkI86pKnmEvtpWz
    > DefaultHttp2Headers[:authority: localhost:8443, :method: DELETE, :path: /webpush/r/s5dcKdO03yK6vBFiwcagkKGkzHVJ8TBs4PJHL0ATVo%2BuYkB5nqDRI%2FYwCqI0eoZ6xvlfaJeb%2BssUqwTgMeBZ%2FE5RMpyLieTY1vG8SVR79SUJtJnDQRQVVsIAoUOxOKkI86pKnmEvtpWz, :scheme: https]
    < [streamId:19] DefaultHttp2Headers[:status: 204, access-control-allow-origin: *]

#### Disconnect

    [webpush]$ disconnect 
    Disconnected from [localhost:8443]
    > Channel with id 308e044e, became inactive/disonnected.

#### Exit the console
    
    > exit
