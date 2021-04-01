package java.nio;

public class ShortBuffer extends Buffer {

    protected short[] data;
    private final int cap;
    private int offset = 0;

    protected ShortBuffer(int size) {
        this.cap = size;
    }
    
    protected ShortBuffer(short[] arr) {
        this.data = arr;
        this.cap = arr.length;
        this.limit(arr.length);
    }

    public static ShortBuffer allocate(int capacity) {
        return new ShortBuffer(new short[capacity]);
    }

    public static ShortBuffer wrap(short[] arr) {
        return new ShortBuffer(arr);
    }

    public boolean isDirect() {
        return false;
    }

    public boolean hasArray() {
        return true;
    }

    @Override
    public short[] array() {
        return data;
    }

    public final int arrayOffset() {
        return offset;
    }

    public ShortBuffer slice() {
        return new ShortBuffer(data);
    }

    public int capacity() {
        return cap;
    }

    @Override
    public ShortBuffer rewind() {
        return (ShortBuffer)super.rewind();
    }

    public int get() {
        return data[nextGetIndex()];
    }

    public short get(int idx) {
        return data[idx];
    }
    
    public ShortBuffer put(short[] src, int offset, int length) {
        for (int i = offset; i < offset + length; i++) {
            data[position++] = src[i];
        }
        return this;
    }

}
