package java.nio;

public class FloatBuffer extends Buffer {

    protected float[] data;
    private final int cap;
    private int offset = 0;

    protected FloatBuffer(int size) {
        this.cap = size;
    }
    
    protected FloatBuffer(float[] arr) {
        this.data = arr;
        this.cap = arr.length;
        this.limit(arr.length);
    }

    public static FloatBuffer allocate(int capacity) {
        return new FloatBuffer(new float[capacity]);
    }

    public static FloatBuffer wrap(float[] arr) {
        return new FloatBuffer(arr);
    }

    public boolean isDirect() {
        return false;
    }

    public boolean hasArray() {
        return true;
    }

    @Override
    public float[] array() {
        return data;
    }

    public final int arrayOffset() {
        return offset;
    }

    public FloatBuffer slice() {
        return new FloatBuffer(data);
    }

    public int capacity() {
        return cap;
    }

    @Override
    public FloatBuffer rewind() {
        return (FloatBuffer)super.rewind();
    }

    public float get() {
        return data[nextGetIndex()];
    }

    public FloatBuffer put(float[] src, int offset, int length) {
        for (int i = offset; i < offset + length; i++) {
            data[position++] = src[i];
        }
        return this;
    }
    
    public FloatBuffer put(int idx, float val) {
        data[idx] = val;
        return this;
    }

    public FloatBuffer put(float f) {
        data[nextPutIndex()] = f;
        return this;
    }

}
