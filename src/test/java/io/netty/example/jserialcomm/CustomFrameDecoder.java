package io.netty.example.jserialcomm;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;
import java.util.zip.CRC32;

public class CustomFrameDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 确保数据至少包含固定头(1) + 指令(1) + 长度域(2) + 校验(2)
        if (in.readableBytes() < 6) {
            return; // 数据不足，等待更多数据
        }

        // 标记当前读指针
        in.markReaderIndex();

        // 检查固定头
        byte header = in.readByte();
        if (header != (byte) 0xEA) {
            throw new IllegalArgumentException("Invalid header: " + header);
        }

        // 读取指令
        byte command = in.readByte();

        // 读取长度域（大端序）
        int length = in.readUnsignedShort();

        // 检查完整帧是否到达
        int totalFrameLength = 1 + 1 + 2 + length + 2; // 固定头 + 指令 + 长度域 + 数据域 + 校验

        // 检查是否有足够的数据
        if (in.readableBytes() < length + 2) {
            in.resetReaderIndex(); // 数据不足，重置读指针
            return;
        }

        // 读取数据域
        ByteBuf data = in.readSlice(length);

        // 读取校验帧
        int receivedChecksum = in.readUnsignedShort();

        // 计算校验和（假设使用 CRC16）
        int calculatedChecksum = calculateChecksum(header, command, length, data);

        // 校验校验和
//        if (receivedChecksum != calculatedChecksum) {
//            throw new IllegalArgumentException(
//                    String.format("Checksum mismatch: expected=0x%04X, actual=0x%04X", calculatedChecksum, receivedChecksum));
//        }

        // 解码成功，构造帧对象
        Frame frame = new Frame(header, command, data.copy(), receivedChecksum);
        out.add(frame);
    }

    private int calculateChecksum(byte header, byte command, int length, ByteBuf data) {
        CRC32 crc32 = new CRC32();
        crc32.update(header);
        crc32.update(command);
        crc32.update((length >> 8) & 0xFF); // 高字节
        crc32.update(length & 0xFF);       // 低字节

        byte[] dataBytes = new byte[data.readableBytes()];
        data.getBytes(data.readerIndex(), dataBytes);
        crc32.update(dataBytes);

        return (int) (crc32.getValue() & 0xFFFF); // 取低16位
    }

}
