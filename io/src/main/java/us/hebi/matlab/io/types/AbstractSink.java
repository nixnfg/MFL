package us.hebi.matlab.io.types;

import us.hebi.matlab.common.memory.ByteConverter;
import us.hebi.matlab.common.memory.ByteConverters;
import us.hebi.matlab.common.memory.Bytes;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import static us.hebi.matlab.common.util.Preconditions.*;
import static us.hebi.matlab.common.memory.Bytes.*;

/**
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 06 May 2018
 */
public abstract class AbstractSink implements Sink {

    @Override
    public void setByteOrder(ByteOrder byteOrder) {
        this.byteOrder = checkNotNull(byteOrder);
    }

    @Override
    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    @Override
    public void writeByte(byte value) throws IOException {
        bytes[0] = value;
        writeBytes(bytes, 0, 1);
    }

    @Override
    public void writeShort(short value) throws IOException {
        byteConverter.putShort(value, getByteOrder(), bytes, 0);
        writeBytes(bytes, 0, SIZEOF_SHORT);
    }

    @Override
    public void writeInt(int value) throws IOException {
        byteConverter.putInt(value, getByteOrder(), bytes, 0);
        writeBytes(bytes, 0, SIZEOF_INT);
    }

    @Override
    public void writeLong(long value) throws IOException {
        byteConverter.putLong(value, getByteOrder(), bytes, 0);
        writeBytes(bytes, 0, SIZEOF_LONG);
    }

    @Override
    public void writeFloat(float value) throws IOException {
        byteConverter.putFloat(value, getByteOrder(), bytes, 0);
        writeBytes(bytes, 0, SIZEOF_FLOAT);
    }

    @Override
    public void writeDouble(double value) throws IOException {
        byteConverter.putDouble(value, getByteOrder(), bytes, 0);
        writeBytes(bytes, 0, SIZEOF_DOUBLE);
    }

    @Override
    public void writeByteBuffer(ByteBuffer buffer) throws IOException {
        if (buffer.hasArray()) {
            // Fast path
            int offset = buffer.arrayOffset() + buffer.position();
            int length = buffer.remaining();
            writeBytes(buffer.array(), offset, length);
            buffer.position(buffer.limit());
        } else {
            // Slow path
            while (buffer.hasRemaining()) {
                int length = Math.min(buffer.remaining(), bytes.length);
                buffer.get(bytes, 0, length);
                writeBytes(bytes, 0, length);
            }

        }
    }

    @Override
    public void writeInputStream(InputStream input, long length) throws IOException {
        for (long i = 0; i < length; ) {
            int maxN = (int) Math.min((length - i), bytes.length);
            int n = input.read(bytes, 0, maxN);
            if (n < 0) throw new EOFException();
            writeBytes(bytes, 0, n);
            i += n;
        }
    }

    @Override
    public void writeDataInput(DataInput input, long length) throws IOException {
        for (long i = 0; i < length; ) {
            int n = (int) Math.min((length - i), bytes.length);
            input.readFully(bytes, 0, n);
            writeBytes(bytes, 0, n);
            i += n;
        }
    }

    @Override
    public void writeShorts(short[] buffer, int offset, int length) throws IOException {
        for (int i = 0; i < length; ) {
            int n = Math.min((length - i) * SIZEOF_SHORT, bytes.length);
            for (int j = 0; j < n; j += SIZEOF_SHORT, i++) {
                byteConverter.putShort(buffer[offset + i], getByteOrder(), bytes, j);
            }
            writeBytes(bytes, 0, n);
        }
    }

    @Override
    public void writeInts(int[] buffer, int offset, int length) throws IOException {
        for (int i = 0; i < length; ) {
            int n = Math.min((length - i) * SIZEOF_INT, bytes.length);
            for (int j = 0; j < n; j += SIZEOF_INT, i++) {
                byteConverter.putInt(buffer[offset + i], getByteOrder(), bytes, j);
            }
            writeBytes(bytes, 0, n);
        }
    }

    @Override
    public void writeLongs(long[] buffer, int offset, int length) throws IOException {
        for (int i = 0; i < length; ) {
            int n = Math.min((length - i) * SIZEOF_LONG, bytes.length);
            for (int j = 0; j < n; j += SIZEOF_LONG, i++) {
                byteConverter.putLong(buffer[offset + i], getByteOrder(), bytes, j);
            }
            writeBytes(bytes, 0, n);
        }
    }

    @Override
    public void writeFloats(float[] buffer, int offset, int length) throws IOException {
        for (int i = 0; i < length; ) {
            int n = Math.min((length - i) * SIZEOF_FLOAT, bytes.length);
            for (int j = 0; j < n; j += SIZEOF_FLOAT, i++) {
                byteConverter.putFloat(buffer[offset + i], getByteOrder(), bytes, j);
            }
            writeBytes(bytes, 0, n);
        }
    }

    @Override
    public void writeDoubles(double[] buffer, int offset, int length) throws IOException {
        for (int i = 0; i < length; ) {
            int n = Math.min((length - i) * SIZEOF_DOUBLE, bytes.length);
            for (int j = 0; j < n; j += SIZEOF_DOUBLE, i++) {
                byteConverter.putDouble(buffer[offset + i], getByteOrder(), bytes, j);
            }
            writeBytes(bytes, 0, n);
        }
    }

    @Override
    public Sink writeDeflated(Deflater deflater) {
        DeflaterOutputStream deflateStream = new DeflaterOutputStream(streamWrapper, deflater, bytes.length);
        int deflateBufferSize = Math.max(1024, bytes.length);
        Sink deflateSink = Sinks.wrapNonSeeking(deflateStream, deflateBufferSize);
        deflateSink.setByteOrder(getByteOrder());
        return deflateSink;
    }

    private OutputStream streamWrapper = new Sinks.SinkOutputStream(this);

    protected AbstractSink(int copyBufferSize) {
        // Make sure size is always a multiple of 8, and that it can hold the 116 byte description
        int size = Math.max(Bytes.nextPowerOfTwo(copyBufferSize), 256);
        this.bytes = new byte[size];
    }

    private ByteOrder byteOrder = ByteOrder.nativeOrder(); // default to native, same as MATLAB
    private final byte[] bytes;
    private static final ByteConverter byteConverter = ByteConverters.getFastest();

}