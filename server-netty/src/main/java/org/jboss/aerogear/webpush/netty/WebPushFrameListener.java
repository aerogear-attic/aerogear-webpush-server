/**
 * JBoss, Home of Professional Open Source
 * Copyright Red Hat, Inc., and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aerogear.webpush.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2FrameAdapter;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.util.AsciiString;
import io.netty.util.AttributeKey;
import io.netty.util.ByteString;
import io.netty.util.concurrent.Future;
import org.jboss.aerogear.webpush.DefaultPushMessage;
import org.jboss.aerogear.webpush.PushMessage;
import org.jboss.aerogear.webpush.Resource;
import org.jboss.aerogear.webpush.Subscription;
import org.jboss.aerogear.webpush.WebLink;
import org.jboss.aerogear.webpush.WebPushServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.netty.handler.codec.http.HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN;
import static io.netty.handler.codec.http.HttpHeaderNames.ACCESS_CONTROL_EXPOSE_HEADERS;
import static io.netty.handler.codec.http.HttpHeaderNames.CACHE_CONTROL;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.GONE;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE;
import static io.netty.util.CharsetUtil.UTF_8;
import static org.jboss.aerogear.webpush.util.HttpHeaders.ALLOW_ORIGIN_ANY;
import static org.jboss.aerogear.webpush.util.HttpHeaders.CACHE_CONTROL_PRIVATE;
import static org.jboss.aerogear.webpush.util.HttpHeaders.CONTENT_TYPE_VALUE;
import static org.jboss.aerogear.webpush.util.HttpHeaders.EXPOSE_HEADERS_CACHE_CONTROL_CONTENT_TYPE_CONTENT_LENGTH;
import static org.jboss.aerogear.webpush.util.HttpHeaders.EXPOSE_HEADERS_LINK_CACHE_CONTROL_LOCATION;
import static org.jboss.aerogear.webpush.util.HttpHeaders.EXPOSE_HEADERS_LOCATION;
import static org.jboss.aerogear.webpush.util.HttpHeaders.LINK_HEADER;
import static org.jboss.aerogear.webpush.util.HttpHeaders.PREFER_HEADER;
import static org.jboss.aerogear.webpush.util.HttpHeaders.PUSH_RECEIPT_HEADER;
import static org.jboss.aerogear.webpush.util.HttpHeaders.TTL_HEADER;

public class WebPushFrameListener extends Http2FrameAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebPushFrameListener.class);

    private static final String WEBPUSH_URI = "/webpush/";

    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final String DELETE = "DELETE";
    private static final AsciiString GET_ASCII = new AsciiString(GET);

    private static final AttributeKey<String> SUBSCRIPTION_ID = AttributeKey.valueOf("SUBSCRIPTION_ID");
    private static final AttributeKey<String> RECEIPT_SUBSCRIPTION_ID = AttributeKey.valueOf("RECEIPT_SUBSCRIPTION_ID");

    private static final ConcurrentHashMap<String, Client> monitoredStreams = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Client> acksStreams = new ConcurrentHashMap<>();

    private final WebPushServer webpushServer;
    private final AsciiString authority;
    private final AsciiString subscriptionMaxAge;

    private Http2ConnectionEncoder encoder;
    private Http2Connection.PropertyKey pathPropertyKey;
    private Http2Connection.PropertyKey resourcePropertyKey;
    private Http2Connection.PropertyKey pushReceiptPropertyKey;
    private Http2Connection.PropertyKey ttlPropertyKey;

    public WebPushFrameListener(final WebPushServer webpushServer) {
        this.webpushServer = Objects.requireNonNull(webpushServer, "webpushServer must not be null");
        this.authority = new AsciiString(webpushServer.config().host() + ":" + webpushServer.config().port());
        this.subscriptionMaxAge = new AsciiString("private, max-age=" + webpushServer.config().subscriptionMaxAge());
    }

    public void encoder(Http2ConnectionEncoder encoder) {
        this.encoder = encoder;
        Http2Connection connection = encoder.connection();
        pathPropertyKey = connection.newKey();
        resourcePropertyKey = connection.newKey();
        pushReceiptPropertyKey = connection.newKey();
        ttlPropertyKey = connection.newKey();
    }

    @Override
    public void onHeadersRead(final ChannelHandlerContext ctx,
                              final int streamId,
                              final Http2Headers headers,
                              final int streamDependency,
                              final short weight,
                              final boolean exclusive,
                              final int padding,
                              final boolean endStream) throws Http2Exception {
        final String path = headers.path().toString();
        final String method = headers.method().toString();
        LOGGER.info("onHeadersRead. streamId={}, method={}, path={}, endstream={}", streamId, method, path, endStream);

        final Resource resource = getResource(path);
        final Http2Stream stream = encoder.connection().stream(streamId);
        stream.setProperty(pathPropertyKey, path);
        stream.setProperty(resourcePropertyKey, resource);
        switch (method) {
            case GET:
                switch (resource) {
                    case SUBSCRIPTION:
                        handleReceivingPushMessages(ctx, streamId, headers, path);
                        return;
                    case RECEIPT:
                        handleReceivingPushMessageReceipts(ctx, streamId, path);
                        return;
                }
                break;
            case POST:
                switch (resource) {
                    case SUBSCRIBE:
                        handleSubscribe(ctx, streamId);
                        return;
                    case RECEIPTS:
                        handleReceipts(ctx, streamId, path);
                        return;
                    case PUSH:
                        final Optional<String> pushReceiptToken = getPushReceiptToken(headers);
                        stream.setProperty(pushReceiptPropertyKey, pushReceiptToken);
                        final Optional<Integer> ttl = getTtl(headers);
                        stream.setProperty(ttlPropertyKey, ttl);
                        //see onDataRead(...) method
                        return;
                }
                break;
            case DELETE:
                switch (resource) {
                    case PUSH_MESSAGE:
                        handleAcknowledgement(ctx, streamId, path);
                        return;
                    case SUBSCRIPTION:
                        handlePushMessageSubscriptionRemoval(ctx, streamId, path);
                        return;
                    case RECEIPT:
                        handleReceiptSubscriptionRemoval(ctx, streamId, path);
                        return;
                }
                break;
        }
    }

    @Override
    public int onDataRead(final ChannelHandlerContext ctx,
                          final int streamId,
                          final ByteBuf data,
                          final int padding,
                          final boolean endOfStream) throws Http2Exception {
        final Http2Stream stream = encoder.connection().stream(streamId);
        final String path = stream.getProperty(pathPropertyKey);
        final Resource resource = stream.getProperty(resourcePropertyKey);
        LOGGER.info("onDataRead. streamId={}, path={}, resource={}, endstream={}", streamId, path, resource,
                endOfStream);
        switch (resource) {
            case PUSH:
                handlePush(ctx, streamId, path, data);
                break;
        }
        return super.onDataRead(ctx, streamId, data, padding, endOfStream);
    }

    private void handleSubscribe(final ChannelHandlerContext ctx, final int streamId) {
        final Subscription subscription = webpushServer.subscribe();
        encoder.writeHeaders(ctx, streamId, subscriptionHeaders(subscription), 0, true, ctx.newPromise());
        LOGGER.info("Subscription for Push Messages: {}", subscription);
    }

    private Http2Headers subscriptionHeaders(final Subscription subscription) {
        final String pushToken = webpushServer.generateEndpointToken(subscription.pushResourceId(), subscription.id());
        final String receiptsToken = webpushServer.generateEndpointToken(subscription.id());
        return resourceHeaders(Resource.SUBSCRIPTION, subscription.id(), EXPOSE_HEADERS_LINK_CACHE_CONTROL_LOCATION)
                .set(LINK_HEADER, asLink(webpushUri(Resource.PUSH, pushToken), WebLink.PUSH),
                                  asLink(webpushUri(Resource.RECEIPTS, receiptsToken), WebLink.RECEIPTS))
                .set(CACHE_CONTROL, subscriptionMaxAge);
    }

    private static AsciiString asLink(final AsciiString uri, WebLink rel) {
        return new AsciiString("<" + uri + ">;rel=\"" + rel + "\"");
    }

    private void handleReceipts(final ChannelHandlerContext ctx, final int streamId, final String path) {
        extractToken(path).flatMap(webpushServer::subscriptionByToken).ifPresent(sub -> {
            final String receiptResourceId = UUID.randomUUID().toString();
            final String receiptResourceToken = webpushServer.generateEndpointToken(receiptResourceId, sub.id());
            encoder.writeHeaders(ctx, streamId, receiptsHeaders(receiptResourceToken), 0, true, ctx.newPromise());
            LOGGER.info("Receipt Subscription Resource: {}", receiptResourceToken);
        });
    }

    private static Http2Headers receiptsHeaders(final String receiptResourceToken) {
        return resourceHeaders(Resource.RECEIPT, receiptResourceToken, EXPOSE_HEADERS_LOCATION);
    }

    private void handlePush(final ChannelHandlerContext ctx,
                            final int streamId,
                            final String path,
                            final ByteBuf data) {
        final Optional<Subscription> subscription = extractToken(path).flatMap(webpushServer::subscriptionByPushToken);
        subscription.ifPresent(sub -> {
            final Http2Stream stream = encoder.connection().stream(streamId);
            final Optional<String> receiptToken = stream.getProperty(pushReceiptPropertyKey);
            if (receiptToken.isPresent()) {
                final Optional<Subscription> receiptSub = webpushServer.subscriptionByReceiptToken(receiptToken.get());
                if (!receiptSub.isPresent() || !subscription.equals(receiptSub)) {
                    badRequest(ctx, streamId, "Subscriptions don't match");
                    return;
                }
            }
            final int readableBytes = data.readableBytes();
            if (readableBytes > webpushServer.config().messageMaxSize()) {
                encoder.writeHeaders(ctx, streamId, messageToLarge(), 0, true, ctx.newPromise());
            } else {
                final PushMessage pushMessage = buildPushMessage(sub.id(), data, stream);
                encoder.writeHeaders(ctx, streamId, pushMessageHeaders(pushMessage), 0, true, ctx.newPromise());
                final Client client = monitoredStreams.get(sub.id());
                if (client != null) {
                    receivePushMessage(pushMessage, client);
                } else {
                    webpushServer.saveMessage(pushMessage);
                    LOGGER.info("UA not connected, saved to storage: {}", pushMessage);
                }
            }
        });
        if (!subscription.isPresent()) {
            encoder.writeHeaders(ctx, streamId, notFoundHeaders(), 0, true, ctx.newPromise());
        }
    }

    private PushMessage buildPushMessage(final String subId, final ByteBuf data, final Http2Stream stream) {
        final String pushMessageId = UUID.randomUUID().toString();
        final Optional<String> receiptToken = stream.getProperty(pushReceiptPropertyKey);
        final Optional<Integer> ttl = stream.getProperty(ttlPropertyKey);
        return new DefaultPushMessage(pushMessageId, subId, receiptToken, data.toString(UTF_8), ttl);
    }

    private Http2Headers pushMessageHeaders(final PushMessage pushMessage) {
        final String pushMessageToken = webpushServer
                .generateEndpointToken(pushMessage.id(), pushMessage.subscription());
        return resourceHeaders(Resource.PUSH_MESSAGE, pushMessageToken, EXPOSE_HEADERS_LOCATION);
    }

    private static Optional<String> getPushReceiptToken(final Http2Headers headers) {
        final ByteString byteString = headers.get(PUSH_RECEIPT_HEADER);
        if (byteString != null) {
            return extractToken(byteString.toString(), Resource.RECEIPT);
        }
        return Optional.empty();
    }

    private static Optional<Integer> getTtl(final Http2Headers headers) {
        final ByteString byteString = headers.get(TTL_HEADER);
        if (byteString != null) {
            Optional.of(byteString.parseAsciiInt());
        }
        return Optional.empty();
    }

    private static Http2Headers messageToLarge() {
        return new DefaultHttp2Headers(false)
                .status(REQUEST_ENTITY_TOO_LARGE.codeAsText())
                .set(ACCESS_CONTROL_ALLOW_ORIGIN, ALLOW_ORIGIN_ANY);
    }

    private void handleReceivingPushMessages(final ChannelHandlerContext ctx,
                                             final int streamId,
                                             final Http2Headers headers,
                                             final String path) {
        extractToken(path).flatMap(webpushServer::subscriptionById).ifPresent(sub -> {
            final Client client = new Client(ctx, streamId, encoder);
            monitoredStreams.put(sub.id(), client); //close the storage
            // all new messages will be sent to the connected client
            try {
                List<PushMessage> newMessages = null;
                while (!(newMessages = webpushServer.waitingDeliveryMessages(sub.id())).isEmpty()) {
                    for (PushMessage pushMessage : newMessages) {
                        receivePushMessage(pushMessage, client);
                    }
                }
                final Optional<ByteString> wait =
                        Optional.ofNullable(headers.get(PREFER_HEADER)).filter(val -> "wait=0".equals(val.toString()));
                if (wait.isPresent()) {
                    monitoredStreams.remove(sub.id());  //open the storage
                    encoder.writeHeaders(ctx, streamId, noContentHeaders(), 0, true, ctx.newPromise());
                    LOGGER.info("204 No Content has sent to client={}", client);
                } else {
                    ctx.attr(SUBSCRIPTION_ID).set(sub.id());
                    LOGGER.info("Registered client={}", client);
                }
            } catch (Exception e) {
                LOGGER.error("Error handleReceivingPushMessages", e);
                monitoredStreams.remove(sub.id());  //open the storage if exception occurred
                //TODO probably, we also should send something to client
            }
        });
    }

    private void receivePushMessage(final PushMessage pushMessage, final Client client) {
        final Http2Headers promiseHeaders = promiseHeaders(pushMessage);
        final Http2Headers monitorHeaders = monitorHeaders(pushMessage);
        final int pushStreamId = client.encoder.connection().local().nextStreamId();
        client.encoder.writePushPromise(client.ctx, client.streamId, pushStreamId, promiseHeaders, 0,
                client.ctx.newPromise()).addListener(WebPushFrameListener::logFutureError);
        client.encoder.writeHeaders(client.ctx, pushStreamId, monitorHeaders, 0, false, client.ctx.newPromise())
                .addListener(WebPushFrameListener::logFutureError);
        client.encoder.writeData(client.ctx, pushStreamId, copiedBuffer(pushMessage.payload(), UTF_8), 0, true,
                client.ctx.newPromise()).addListener(WebPushFrameListener::logFutureError);
        client.ctx.flush(); //FIXME flush at the end of all writes
        LOGGER.info("Sent to client={}, pushPromiseStreamId={}, promiseHeaders={}, monitorHeaders={}, pushMessage={}",
                client, pushStreamId, promiseHeaders, monitorHeaders, pushMessage);

        pushMessage.receiptSubscription().ifPresent(rs -> webpushServer.saveSentMessage(pushMessage));
    }

    private Http2Headers monitorHeaders(final PushMessage pushMessage) {
        return new DefaultHttp2Headers(false)
                .status(OK.codeAsText())
                .set(ACCESS_CONTROL_ALLOW_ORIGIN, ALLOW_ORIGIN_ANY)
                .set(ACCESS_CONTROL_EXPOSE_HEADERS, EXPOSE_HEADERS_CACHE_CONTROL_CONTENT_TYPE_CONTENT_LENGTH)
                .set(CACHE_CONTROL, CACHE_CONTROL_PRIVATE)
                .set(CONTENT_TYPE, CONTENT_TYPE_VALUE)
                .setInt(CONTENT_LENGTH, pushMessage.payload().length());
        //TODO add "last-modified" headers
    }

    private void handleAcknowledgement(final ChannelHandlerContext ctx, final int streamId, final String path) {
        extractToken(path).flatMap(webpushServer::sentMessage).ifPresent(pushMessage -> {
            final Client client = acksStreams.get(pushMessage.receiptSubscription().get());
            if (client != null) {
                receivePushMessageReceipts(pushMessage, client);
            }
        });
        //FIXME should I send a response to a UA?
    }

    private void receivePushMessageReceipts(final PushMessage pushMessage, final Client client) {
        final Http2Headers promiseHeaders = promiseHeaders(pushMessage);
        final Http2Headers ackHeaders = goneHeaders();
        final int pushStreamId = client.encoder.connection().local().nextStreamId();
        client.encoder.writePushPromise(client.ctx, client.streamId, pushStreamId, promiseHeaders, 0,
                client.ctx.newPromise()).addListener(WebPushFrameListener::logFutureError);
        client.encoder.writeHeaders(client.ctx, pushStreamId, ackHeaders, 0, true,
                client.ctx.newPromise()).addListener(WebPushFrameListener::logFutureError);
        client.ctx.flush();
        LOGGER.info("Sent ack to client={}, pushPromiseStreamId={}, promiseHeaders={}, ackHeaders={}, pushMessage={}",
                client, pushStreamId, promiseHeaders, ackHeaders, pushMessage);
    }

    private void handleReceivingPushMessageReceipts(final ChannelHandlerContext ctx,
                                                    final int streamId,
                                                    final String path) {
        final Optional<String> receiptToken = extractToken(path);
        receiptToken.flatMap(webpushServer::subscriptionByReceiptToken).ifPresent(sub -> {
            final Client client = new Client(ctx, streamId, encoder);
            acksStreams.put(receiptToken.get(), client);
            ctx.attr(RECEIPT_SUBSCRIPTION_ID).set(receiptToken.get());
            LOGGER.info("Registered application for acks={}", client);
        });
    }

    private void handlePushMessageSubscriptionRemoval(final ChannelHandlerContext ctx,
                                                      final int streamId,
                                                      final String path) {
        final String subId = extractEndpointToken(path);
        final List<PushMessage> sentMessages = webpushServer.removeSubscription(subId);
        removeClient(Optional.ofNullable(subId), monitoredStreams);
        sentMessages.forEach(sm -> {
            removeClient(sm.receiptSubscription(), acksStreams);
        });
        LOGGER.info("Subscription {} removed", subId);
        encoder.writeHeaders(ctx, streamId, noContentHeaders(), 0, true, ctx.newPromise());
    }

    private void handleReceiptSubscriptionRemoval(final ChannelHandlerContext ctx,
                                                  final int streamId,
                                                  final String path) {
        final Client client = extractToken(path).map(acksStreams::remove).orElse(null);
        if (client != null) {
            ctx.attr(RECEIPT_SUBSCRIPTION_ID).remove();
            LOGGER.info("Removed application server registration for acks={}", client);
        }
        encoder.writeHeaders(ctx, streamId, noContentHeaders(), 0, true, ctx.newPromise());
    }

    private static Http2Headers resourceHeaders(final Resource resource,
                                                final String resourceToken,
                                                final AsciiString exposeHeaders) {
        return new DefaultHttp2Headers(false)
                .status(CREATED.codeAsText())
                .set(ACCESS_CONTROL_ALLOW_ORIGIN, ALLOW_ORIGIN_ANY)
                .set(ACCESS_CONTROL_EXPOSE_HEADERS, exposeHeaders)
                .set(LOCATION, webpushUri(resource, resourceToken));
    }

    private static AsciiString webpushUri(final Resource resource, final String id) {
        return new AsciiString(WEBPUSH_URI + resource.resourceName() + "/" + id);
    }

    private static Optional<String> extractToken(final String path, final Resource resource) {
        final String segment = WEBPUSH_URI + resource.resourceName();
        int idx = path.indexOf(segment);
        if (idx < 0) {
            return Optional.empty();
        }
        final String subpath = path.substring(idx + segment.length());
        return extractToken(subpath);
    }

    private static Optional<String> extractToken(final String path) {
        final int idx = path.lastIndexOf('/');
        if (idx < 0) {
            return Optional.empty();
        }
        return Optional.of(path.substring(idx + 1));
    }

    private static String extractEndpointToken(final String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private static Resource getResource(final String path) {
        final String resourceName;
        final int idx = path.indexOf('/', WEBPUSH_URI.length());
        if (idx > 0) {
            resourceName = path.substring(WEBPUSH_URI.length(), idx);
        } else {
            resourceName = path.substring(WEBPUSH_URI.length());
        }
        return Resource.byResourceName(resourceName);
    }

    private static Http2Headers goneHeaders() {
        return new DefaultHttp2Headers(false)
                .status(GONE.codeAsText())
                .set(ACCESS_CONTROL_ALLOW_ORIGIN, ALLOW_ORIGIN_ANY);  //FIXME add date
    }

    private static Http2Headers noContentHeaders() {
        return new DefaultHttp2Headers(false)
                .status(NO_CONTENT.codeAsText())
                .set(ACCESS_CONTROL_ALLOW_ORIGIN, ALLOW_ORIGIN_ANY);
    }

    private static Http2Headers notFoundHeaders() {
        return new DefaultHttp2Headers(false)
                .status(NOT_FOUND.codeAsText())
                .set(ACCESS_CONTROL_ALLOW_ORIGIN, ALLOW_ORIGIN_ANY);
    }

    private static Http2Headers badRequestHeaders() {
        return new DefaultHttp2Headers(false)
                .status(BAD_REQUEST.codeAsText())
                .set(ACCESS_CONTROL_ALLOW_ORIGIN, ALLOW_ORIGIN_ANY);
    }

    private void badRequest(final ChannelHandlerContext ctx, final int streamId, final String errorMsg) {
        encoder.writeHeaders(ctx, streamId, badRequestHeaders(), 0, false, ctx.newPromise());
        encoder.writeData(ctx, streamId, copiedBuffer(errorMsg, UTF_8), 0, true, ctx.newPromise());
    }

    private Http2Headers promiseHeaders(final PushMessage pushMessage) {
        final String token = webpushServer.generateEndpointToken(pushMessage.id(), pushMessage.subscription());
        return new DefaultHttp2Headers(false)
                .method(GET_ASCII)
                .path(webpushUri(Resource.PUSH_MESSAGE, token))
                .authority(authority);
    }

    public void shutdown() {
        monitoredStreams.values().stream().forEach(client -> client.ctx.close());
        monitoredStreams.clear();
        acksStreams.values().stream().forEach(client -> client.ctx.close());
        acksStreams.clear();
    }

    public void disconnect(final ChannelHandlerContext ctx) {
        final Optional<String> subId = Optional.ofNullable(ctx.attr(SUBSCRIPTION_ID).get());
        removeClient(subId, monitoredStreams);
        final Optional<String> recSubId = Optional.ofNullable(ctx.attr(RECEIPT_SUBSCRIPTION_ID).get());
        removeClient(recSubId, acksStreams);
        LOGGER.info("Disconnected channel {}", ctx.channel().id());
    }

    private static void removeClient(final Optional<String> idOpt, final Map<String, Client> map) {
        idOpt.ifPresent(id -> {
            final Client client = map.remove(id);
            if (client != null) {
                client.encoder.writeHeaders(client.ctx, client.streamId, goneHeaders(), 0, true,
                        client.ctx.newPromise());
                client.ctx.flush();
                LOGGER.info("Removed client={}", client);
            }
        });
    }

    private static void logFutureError(final Future future) {
        if (!future.isSuccess()) {
            LOGGER.error("ChannelFuture failed. Cause:", future.cause());
        }
    }

    private static class Client {

        private final ChannelHandlerContext ctx;
        private final Http2ConnectionEncoder encoder;
        private final int streamId;

        Client(final ChannelHandlerContext ctx, final int streamId, final Http2ConnectionEncoder encoder) {
            this.ctx = ctx;
            this.streamId = streamId;
            this.encoder = encoder;
        }

        @Override
        public String toString() {
            return "Client[streamid=" + streamId + ", ctx=" + ctx + "]";
        }
    }
}
