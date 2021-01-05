package java.nio;

public class ByteBuffer extends Buffer {

    private byte[] data;
    private int offset = 0;
    boolean bigEndian = true;
    boolean nativeByteOrder = (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN);


    protected ByteBuffer(byte[] arr) {
        this.data = arr;
        this.limit(arr.length);
    }

    public static ByteBuffer allocate(int capacity) {
        return new ByteBuffer(new byte[capacity]);
    }

    public static ByteBuffer allocateDirect(int capacity) {
        return allocate(capacity);
    }

    public static ByteBuffer wrap(byte[] arr) {
        return new ByteBuffer(arr);
    }

    public boolean isDirect() {
        return true;
    }

    public boolean hasArray() {
        return true;
    }

    public byte[] array() {
        return data;
    }

    @Override
    public int arrayOffset() {
        return this.offset;
    }

    @Override
    public ByteBuffer rewind() {
        super.rewind();
        return this;
    }

    public final ByteOrder order() {
        return bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
    }

    public final ByteBuffer order(ByteOrder bo) {
        bigEndian = (bo == ByteOrder.BIG_ENDIAN);
        nativeByteOrder =
            (bigEndian == (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN));
        return this;
    }
}
