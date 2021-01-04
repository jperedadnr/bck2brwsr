package java.nio;

public class ByteBuffer extends Buffer {

    private byte[] data;
    private int offset = 0;

    protected ByteBuffer(byte[] arr) {
        this.data = arr;
    }

    public static ByteBuffer allocate(int capacity) {
        return new ByteBuffer(new byte[capacity]);
    }

    public static ByteBuffer wrap(byte[] arr) {
        return new ByteBuffer(arr);
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

}
