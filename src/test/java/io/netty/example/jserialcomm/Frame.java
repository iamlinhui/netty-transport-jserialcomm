package io.netty.example.jserialcomm;

import io.netty.buffer.ByteBuf;

public class Frame {
    private final byte header;
    private final byte command;
    private final ByteBuf data;
    private final int checksum;

    public Frame(byte header, byte command, ByteBuf data, int checksum) {
        this.header = header;
        this.command = command;
        this.data = data;
        this.checksum = checksum;
    }

    // Getters
    public byte getHeader() { return header; }
    public byte getCommand() { return command; }
    public ByteBuf getData() { return data; }
    public int getChecksum() { return checksum; }

    @Override
    public String toString() {
        return String.format("Frame[header=0x%02X, command=0x%02X, data=%s, checksum=0x%04X]",
                header, command, data.toString(), checksum);
    }
}
