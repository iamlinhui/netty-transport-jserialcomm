/*
 * Copyright 2017 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.channel.jsc;

import com.fazecast.jSerialComm.SerialPort;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.oio.OioByteStreamChannel;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import static io.netty.channel.jsc.JSerialCommChannelOption.*;


/**
 * A channel to a serial device using the jSerialComm library.
 */
public class JSerialCommChannel extends OioByteStreamChannel {

    private static final JSerialCommDeviceAddress LOCAL_ADDRESS = new JSerialCommDeviceAddress("localhost");

    private final JSerialCommChannelConfig config;

    private boolean open = true;
    private JSerialCommDeviceAddress deviceAddress;
    private SerialPort serialPort;

    private InputStream is;

    public JSerialCommChannel() {
        super(null);

        config = new DefaultJSerialCommChannelConfig(this);
    }

    @Override
    public JSerialCommChannelConfig config() {
        return config;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    protected AbstractUnsafe newUnsafe() {
        return new JSCUnsafe();
    }

    /**
     * 重载doReadBytes函数，捕获因为串口没有数据读取，buf.writeBytes操作产生异常
     * Netty的默认处理是关闭连接
     * 此处理不妥，很多时候串口不一定有心跳数据，很长时间也不一定有数据，但是也不能关闭连接
     * 此处自定义JSerialCommReadTimeoutException，将该异常抛出到串口应用程序，然后依据业务实际情况进行处理
     * @param buf
     * @return
     * @throws Exception
     */
    @Override
    protected int doReadBytes(ByteBuf buf) throws Exception {
        final RecvByteBufAllocator.Handle allocHandle = unsafe().recvBufAllocHandle();
        allocHandle.attemptedBytesRead(Math.max(1, Math.min(available(), buf.maxWritableBytes())));

        is=serialPort.getInputStream();
        int i=allocHandle.attemptedBytesRead();
        try {
            return buf.writeBytes(is, i);
        }
        catch (IOException io)
        {
            //串口在给定时间内没数据读取
            //抛出自定义串口读取超时异常处理异常JSerialCommReadTimeoutException
            throw new JSerialCommReadTimeoutException("The Serial read operation timed out before any data was returned");
        }
    }

    @Override
    protected void doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
        JSerialCommDeviceAddress remote = (JSerialCommDeviceAddress) remoteAddress;
        SerialPort commPort = SerialPort.getCommPort(remote.value());
        if (!commPort.openPort()) {
            throw new IOException("Could not open port: " + remote.value());
        }

        commPort.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_BLOCKING, config().getOption(READ_TIMEOUT), 0);

        deviceAddress = remote;
        serialPort = commPort;
    }

    protected void doInit() throws Exception {
        serialPort.setComPortParameters(
            config().getOption(BAUD_RATE),
            config().getOption(DATA_BITS),
            config().getOption(STOP_BITS).value(),
            config().getOption(PARITY_BIT).value()
        );

        activate(serialPort.getInputStream(), serialPort.getOutputStream());
    }

    @Override
    public JSerialCommDeviceAddress localAddress() {
        return (JSerialCommDeviceAddress) super.localAddress();
    }

    @Override
    public JSerialCommDeviceAddress remoteAddress() {
        return (JSerialCommDeviceAddress) super.remoteAddress();
    }

    @Override
    protected JSerialCommDeviceAddress localAddress0() {
        return LOCAL_ADDRESS;
    }

    @Override
    protected JSerialCommDeviceAddress remoteAddress0() {
        return deviceAddress;
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doDisconnect() throws Exception {
        doClose();
    }

    @Override
    protected void doClose() throws Exception {
        open = false;
        try {
           super.doClose();
        } finally {
            if (serialPort != null) {
                serialPort.closePort();
                serialPort = null;
            }
        }
    }

    @Override
    protected boolean isInputShutdown() {
        return !open;
    }

    @Override
    protected ChannelFuture shutdownInput() {
        return newFailedFuture(new UnsupportedOperationException("shutdownInput"));
    }


    private final class JSCUnsafe extends AbstractUnsafe {
        @Override
        public void connect(
                final SocketAddress remoteAddress,
                final SocketAddress localAddress, final ChannelPromise promise) {
            if (!promise.setUncancellable() || !isOpen()) {
                return;
            }

            try {
                final boolean wasActive = isActive();
                doConnect(remoteAddress, localAddress);

                int waitTime = config().getOption(WAIT_TIME);
                if (waitTime > 0) {
                    eventLoop().schedule(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                doInit();
                                safeSetSuccess(promise);
                                if (!wasActive && isActive()) {
                                    pipeline().fireChannelActive();
                                }
                            } catch (Throwable t) {
                                safeSetFailure(promise, t);
                                closeIfClosed();
                            }
                        }
                   }, waitTime, TimeUnit.MILLISECONDS);
                } else {
                    doInit();
                    safeSetSuccess(promise);
                    if (!wasActive && isActive()) {
                        pipeline().fireChannelActive();
                    }
                }
            } catch (Throwable t) {
                safeSetFailure(promise, t);
                closeIfClosed();
            }
        }
    }
}
