/*
 * Copyright 2017 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.example.jserialcomm;

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.concurrent.TimeUnit;

public class JSerialCommFrameClientHandler extends SimpleChannelInboundHandler<Frame> {

    private final String cmd = "ea0c00013131";

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(ByteUtils.parseHexStrToByte(cmd));
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Frame msg) throws Exception {
        byte[] bytes = ByteBufUtil.getBytes(msg.getData());
        String s = new String(bytes);
        System.out.println(s);
        TimeUnit.MILLISECONDS.sleep(66);
        ctx.writeAndFlush(ByteUtils.parseHexStrToByte(cmd));
    }
}
