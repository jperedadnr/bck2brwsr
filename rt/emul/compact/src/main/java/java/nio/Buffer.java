package java.nio;

public abstract class Buffer {

    protected int position = 0;
    protected int limit = 0;

    public abstract Object array();
    public abstract int arrayOffset();

    public Buffer rewind() {
        this.position = 0;
        return this;
    }

    public final boolean hasRemaining() {
        return position < limit;
    }

    public final int remaining() {
        return limit - position;
    }

    public Buffer limit(int newLimit) {
        limit = newLimit;
        return this;
    }

    int nextGetIndex() {
        int answer = position;
        position++;
        return answer;
    }


}
