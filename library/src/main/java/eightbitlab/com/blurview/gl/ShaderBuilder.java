package eightbitlab.com.blurview.gl;

import android.opengl.GLES20;
import android.util.Log;

class ShaderBuilder {

    static int buildProgram(String vertexSource, String fragmentSource) {
        final int vertexShader = buildShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }

        final int fragmentShader = buildShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (fragmentShader == 0) {
            return 0;
        }

        final int program = GLES20.glCreateProgram();
        if (program == 0) {
            return 0;
        }

        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        return program;
    }

    private static int buildShader(int type, String shaderSource) {
        final int shader = GLES20.glCreateShader(type);
        if (shader == 0) {
            return 0;
        }

        GLES20.glShaderSource(shader, shaderSource);
        GLES20.glCompileShader(shader);

        int[] status = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            Log.e(BlurShader.class.getSimpleName(), GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }

        return shader;
    }
}
