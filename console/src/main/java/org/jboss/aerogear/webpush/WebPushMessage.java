package org.jboss.aerogear.webpush;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http2.Http2Headers;

public class WebPushMessage {

    private final Http2Headers headers;
    private final ByteBuf payload;

    public WebPushMessage(final Http2Headers headers) {
        this(headers, null);
    }

    public WebPushMessage(final Http2Headers headers, final ByteBuf payload) {
        this.headers = headers;
        this.payload = payload;
    }

    public Http2Headers headers() {
        return headers;
    }

    public boolean hasData() {
        return payload != null;
    }

    public ByteBuf payload() {
        return payload;
    }

    @Override
    public String toString() {
        return "WebPushMessage[header=" + headers + ", payload=" + payload + "]";
    }
}
