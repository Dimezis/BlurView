package eightbitlab.com.blurview.gl;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author nekocode (nekocode.cn@gmail.com)
 */
class RenderBuffer {

    private final SizeProvider sizeProvider;

    private int texId = 0;
    private int activeTexUnit = 0;
    private int renderBufferId = 0;
    private int frameBufferId = 0;

    private int width, height;

    RenderBuffer(int width, int height, int activeTexUnit, SizeProvider sizeProvider) {
        this.width = width;
        this.height = height;
        this.activeTexUnit = activeTexUnit;
        this.sizeProvider = sizeProvider;
        int[] bufferHolder = new int[1];

        // Generate and bind 2d texture
        GLES20.glActiveTexture(activeTexUnit);
        texId = genTexture();
        IntBuffer texBuffer =
                ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, texBuffer);

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        // Generate frame buffer
        GLES20.glGenFramebuffers(1, bufferHolder, 0);
        frameBufferId = bufferHolder[0];
        // Bind frame buffer
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufferId);

        // Generate render buffer
        GLES20.glGenRenderbuffers(1, bufferHolder, 0);
        renderBufferId = bufferHolder[0];
        // Bind render buffer
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, renderBufferId);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, width, height);

        unbind();
    }

    int getTexId() {
        return texId;
    }

    int getActiveTexUnit() {
        return activeTexUnit;
    }

    void bind() {
        GLES20.glViewport(0, 0, width, height);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufferId);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, texId, 0);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
                GLES20.GL_RENDERBUFFER, renderBufferId);
    }

    void unbind() {
        //TODO consider passing values rather than a sizeProvider
        GLES20.glViewport(0, 0, sizeProvider.width(), sizeProvider.height());
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    private static int genTexture() {
        return genTexture(GLES20.GL_TEXTURE_2D);
    }

    private static int genTexture(int textureType) {
        int[] bufferHolder = new int[1];
        GLES20.glGenTextures(1, bufferHolder, 0);
        GLES20.glBindTexture(textureType, bufferHolder[0]);

        // Set texture default draw parameters
        if (textureType == GLES11Ext.GL_TEXTURE_EXTERNAL_OES) {
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        } else {
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);
        }

        return bufferHolder[0];
    }
}
