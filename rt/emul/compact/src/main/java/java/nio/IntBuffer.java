package java.nio;

public class IntBuffer extends Buffer {

    protected int[] data;
    private final int cap;
    private int offset = 0;

    protected IntBuffer(int size) {
        this.cap = size;
    }
    
    protected IntBuffer(int[] arr) {
        this.data = arr;
        this.cap = arr.length;
        this.limit(arr.length);
    }

    public static IntBuffer allocate(int capacity) {
        return new IntBuffer(new int[capacity]);
    }

    public static IntBuffer wrap(int[] arr) {
        return new IntBuffer(arr);
    }

    public boolean isDirect() {
        return false;
    }

    public boolean hasArray() {
        return true;
    }

    @Override
    public int[] array() {
        return data;
    }

    public final int arrayOffset() {
        return offset;
    }

    public IntBuffer slice() {
        return new IntBuffer(data);
    }

    public int capacity() {
        return cap;
    }

    @Override
    public IntBuffer rewind() {
        return (IntBuffer)super.rewind();
    }

    public int get() {
        return data[nextGetIndex()];
    }

    public IntBuffer put(int[] src, int offset, int length) {
        for (int i = offset; i < offset + length; i++) {
            data[position++] = src[i];
        }
        return this;
    }
    
    public IntBuffer put(int idx, int val) {
        data[idx] = val;
        return this;
    }

}
