package org.jboss.aerogear.webpush;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http2.Http2Headers;

/**
 * Allows a WebPush client the ability to handle responses from the WebPush Server.
 */
public interface EventHandler {

    /**
     * Fired before an outbound event occurs.
     *
     * @param headers the headers of the outbound request
     */
    void outbound(Http2Headers headers);

    /**
     * Fired before an outbound event occurs.
     *
     * @param headers the headers of the outbound request
     * @param payload the headers of the outbound request
     */
    void outbound(Http2Headers headers, ByteBuf payload);

    /**
     * Fired after an inbound event occurs.
     *
     * @param headers the headers returned from the register request.
     * @param streamId the streamId for this response.
     */
    void inbound(Http2Headers headers, int streamId);

    /**
     * Notifications send from the WebPush server
     *
     * @param data the body of the application server PUT request.
     * @param streamId the streamId for this notification.
     */
    void notification(String data, int streamId);

}
