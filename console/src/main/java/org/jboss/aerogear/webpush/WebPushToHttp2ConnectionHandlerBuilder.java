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
package org.jboss.aerogear.webpush;

import io.netty.handler.codec.http2.AbstractHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.Http2Settings;

public class WebPushToHttp2ConnectionHandlerBuilder extends
        AbstractHttp2ConnectionHandlerBuilder<WebPushToHttp2ConnectionHandler, WebPushToHttp2ConnectionHandlerBuilder> {

    @Override
    public WebPushToHttp2ConnectionHandlerBuilder frameListener(final Http2FrameListener frameListener) {
        return super.frameListener(frameListener);
    }

    @Override
    public WebPushToHttp2ConnectionHandlerBuilder connection(final Http2Connection connection) {
        return super.connection(connection);
    }

    @Override
    public WebPushToHttp2ConnectionHandler build() {
        return super.build();
    }

    @Override
    protected WebPushToHttp2ConnectionHandler build(final Http2ConnectionDecoder decoder,
                                                    final Http2ConnectionEncoder encoder,
                                                    final Http2Settings initialSettings) throws Exception {
        return new WebPushToHttp2ConnectionHandler(decoder, encoder, initialSettings);
    }
}
