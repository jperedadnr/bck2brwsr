package java.nio;

public abstract class Buffer {

    protected int position = 0;

    public abstract Object array();
    public abstract int arrayOffset();

    public Buffer rewind() {
        this.position = 0;
        return this;
    }

}
