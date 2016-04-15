/**
 * JBoss, Home of Professional Open Source
 * Copyright Red Hat, Inc., and individual contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aerogear.webpush.netty;

import io.netty.handler.codec.http2.AbstractHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Settings;
import org.jboss.aerogear.webpush.WebPushServer;

import static io.netty.util.internal.ObjectUtil.checkNotNull;

public class WebPushHttp2HandlerBuilder
        extends AbstractHttp2ConnectionHandlerBuilder<WebPushHttp2Handler, WebPushHttp2HandlerBuilder> {

    private WebPushServer webpushServer;

    public WebPushHttp2HandlerBuilder webPushServer(final WebPushServer webpushServer) {
        this.webpushServer = checkNotNull(webpushServer, "webpushServer");
        return self();
    }

    @Override
    public WebPushHttp2Handler build() {
        return super.build();
    }

    @Override
    protected WebPushHttp2Handler build(final Http2ConnectionDecoder decoder,
                                        final Http2ConnectionEncoder encoder,
                                        final Http2Settings initialSettings) throws Exception {
        if (webpushServer == null) {
            throw new IllegalStateException("WebPushServer was not specified");
        }
        WebPushFrameListener listener = new WebPushFrameListener(webpushServer, encoder);
        frameListener(listener);
        return new WebPushHttp2Handler(decoder, encoder, initialSettings, listener);
    }
}
