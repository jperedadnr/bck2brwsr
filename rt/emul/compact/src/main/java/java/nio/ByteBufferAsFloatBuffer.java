package java.nio;

class ByteBufferAsFloatBuffer extends ShortBuffer {

    protected final ByteBuffer bb;

    ByteBufferAsFloatBuffer(ByteBuffer bb) {
        super(bb.array().length/4);
        this.bb = bb;
        byte[] rb = bb.array();
        float[] sh = new float[bb.array().length/4];
        for (int i = 0; i < sh.length; i++ ) {
            sh[i] = rb[4*i] <<24 + rb[4*i+1] << 16 + rb[4*i+2]<<8 + rb[4*i+3];
        }
        data = sh;
        System.err.println("WARNING: BYTEBUFFERASFLOATBUFFER mainly unimplemented");
    }

}