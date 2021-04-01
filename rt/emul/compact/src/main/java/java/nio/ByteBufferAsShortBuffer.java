package java.nio;

class ByteBufferAsShortBuffer extends ShortBuffer {
    
    protected final ByteBuffer bb;

    ByteBufferAsShortBuffer(ByteBuffer bb) {
        super(bb.array().length/2);
        byte[] rb = bb.array();
        short[] sh = new short[bb.array().length/2];
        for (int i = 0; i < sh.length; i++ ) {
            sh[i] = (short)(rb[2*i] * 256 + rb[2*i+1]);
        }
        data = sh;
        this.bb = bb;
        System.err.println("WARNING: BYTEBUFFERASSHORTBUFFER mainly unimplemented");
    }
}