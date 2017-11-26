package eightbitlab.com.blurview.gl;

import android.annotation.SuppressLint;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static java.lang.Math.PI;
import static java.lang.Math.exp;
import static java.lang.Math.floor;
import static java.lang.Math.log;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static java.lang.String.format;

//Shader idea mostly taken from iOS GPUImage lib by Brad Larson
//TODO use constant names for shader uniforms and attributes

@SuppressLint("DefaultLocale")
final class BlurShader {

    private static final String IMAGE_TEXTURE = "inputImageTexture";
    private static final String POSITION = "position";
    private static final String TEXTURE_COORDINATES = "inputTextureCoordinates";

    private static final FloatBuffer VERTEX_BUFFER;
    private static final FloatBuffer TEXTURE_COORD_BUFFER;

    private static final int DEFAULT_BLUR_RADIUS = 8;

    private final int width;
    private final int height;
    private final int textureId;
    private final RenderBuffer renderBuffer;

    static {
        float squareCoords[] = {
                1.0f, -1.0f,
                -1.0f, -1.0f,
                1.0f, 1.0f,
                -1.0f, 1.0f,
        };

        VERTEX_BUFFER = ByteBuffer.allocateDirect(squareCoords.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        VERTEX_BUFFER.put(squareCoords);
        VERTEX_BUFFER.position(0);

        float textureCoords[] = {
                1.0f, 1.0f,
                0.0f, 1.0f,
                1.0f, 0.0f,
                0.0f, 0.0f,
        };

        TEXTURE_COORD_BUFFER = ByteBuffer.allocateDirect(textureCoords.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        TEXTURE_COORD_BUFFER.put(textureCoords);
        TEXTURE_COORD_BUFFER.position(0);
    }

    private int firstPassProgram;
    private int secondPassProgram;
    private int blurRadius;

    BlurShader(int width, int height, int textureId, SizeProvider sizeProvider) {
        this.width = width;
        this.height = height;
        this.textureId = textureId;
        renderBuffer = new RenderBuffer(width, height, GLES20.GL_TEXTURE0, sizeProvider);

        setBlurRadius(DEFAULT_BLUR_RADIUS);
    }

    void draw() {
        renderBuffer.bind();
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        setupAndDraw(textureId, GLES11Ext.GL_TEXTURE_EXTERNAL_OES, firstPassProgram);

        renderBuffer.unbind();
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        setupAndDraw(renderBuffer.getTexId(), GLES20.GL_TEXTURE_2D, secondPassProgram);
    }

    private void setupAndDraw(int textureId, int target, int program) {
        GLES20.glUseProgram(program);

        int textureLocation = GLES20.glGetUniformLocation(program, IMAGE_TEXTURE);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(target, textureId);
        GLES20.glUniform1i(textureLocation, 0);

        int positionLocation = GLES20.glGetAttribLocation(program, POSITION);
        GLES20.glEnableVertexAttribArray(positionLocation);
        GLES20.glVertexAttribPointer(positionLocation, 2, GLES20.GL_FLOAT, false, 4 * 2, VERTEX_BUFFER);

        int textureCoordinateLocation = GLES20.glGetAttribLocation(program, TEXTURE_COORDINATES);
        GLES20.glEnableVertexAttribArray(textureCoordinateLocation);
        GLES20.glVertexAttribPointer(textureCoordinateLocation, 2, GLES20.GL_FLOAT, false, 4 * 2, TEXTURE_COORD_BUFFER);

        // Render to screen
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    void setBlurRadius(int blurRadius) {
        if (blurRadius == 0) {
            throw new IllegalArgumentException("Blur radius can't be 0");
        }
        if (this.blurRadius == blurRadius) {
            return;
        }

        this.blurRadius = blurRadius;

        int calculatedSampleRadius;
        // Calculate the number of pixels to sample from by setting a bottom limit for the contribution of the outermost pixel
        float minimumWeightToFindEdgeOfSamplingArea = 1f / 256f;
        calculatedSampleRadius = (int) floor(sqrt(-2.0 * pow(blurRadius, 2.0) * log(minimumWeightToFindEdgeOfSamplingArea * sqrt(2.0 * PI * pow(blurRadius, 2.0)))));
        calculatedSampleRadius += calculatedSampleRadius % 2; // There's nothing to gain from handling odd radius sizes, due to the optimizations used

        //Input radius (blurRadius) is a sigma for Gaussian equation, and (calculatedSampleRadius) is a real blur radius
        firstPassProgram = ShaderBuilder.buildProgram(buildVertexShader(calculatedSampleRadius, blurRadius, false), buildFragmentShader(calculatedSampleRadius, blurRadius, false));
        secondPassProgram = ShaderBuilder.buildProgram(buildVertexShader(calculatedSampleRadius, blurRadius, true), buildFragmentShader(calculatedSampleRadius, blurRadius, true));
    }

    private String buildVertexShader(int blurRadius, float sigma, boolean sourceIsFbo) {
        // First, generate the normal Gaussian weights for a given sigma
        float[] standardGaussianWeights = new float[blurRadius + 1];
        float sumOfWeights = 0f;
        for (int currentGaussianWeightIndex = 0; currentGaussianWeightIndex < blurRadius + 1; currentGaussianWeightIndex++) {
            standardGaussianWeights[currentGaussianWeightIndex] = (float) ((1.0 / sqrt(2.0 * PI * pow(sigma, 2.0))) * exp(-pow(currentGaussianWeightIndex, 2.0) / (2.0 * pow(sigma, 2.0))));

            if (currentGaussianWeightIndex == 0) {
                sumOfWeights += standardGaussianWeights[currentGaussianWeightIndex];
            } else {
                sumOfWeights += 2.0 * standardGaussianWeights[currentGaussianWeightIndex];
            }
        }

        // Next, normalize these weights to prevent the clipping of the Gaussian curve at the end of the discrete samples from reducing luminance
        for (int currentGaussianWeightIndex = 0; currentGaussianWeightIndex < blurRadius + 1; currentGaussianWeightIndex++) {
            standardGaussianWeights[currentGaussianWeightIndex] = standardGaussianWeights[currentGaussianWeightIndex] / sumOfWeights;
        }

        // From these weights we calculate the offsets to read interpolated values from
        int numberOfOptimizedOffsets = min(blurRadius / 2 + (blurRadius % 2), 7);
        float[] optimizedGaussianOffsets = new float[numberOfOptimizedOffsets];

        for (int currentOptimizedOffset = 0; currentOptimizedOffset < numberOfOptimizedOffsets; currentOptimizedOffset++) {
            float firstWeight = standardGaussianWeights[currentOptimizedOffset * 2 + 1];
            float secondWeight = standardGaussianWeights[currentOptimizedOffset * 2 + 2];

            float optimizedWeight = firstWeight + secondWeight;

            optimizedGaussianOffsets[currentOptimizedOffset] = (firstWeight * (currentOptimizedOffset * 2 + 1) + secondWeight * (currentOptimizedOffset * 2 + 2)) / optimizedWeight;
        }

        String position;
        String singleStepOffset;

        if (sourceIsFbo) {
            //FBO comes flipped vertically, so we need to flip it back, scaling Y by (-1)
            position = "gl_Position = vec4(position.x, position.y * -1.0, 0.0, 1.0);\n";
            singleStepOffset = format("vec2(0.0, %f);\n", 1.0 / height);
        } else {
            position = "gl_Position = position;\n";
            singleStepOffset = format("vec2(%f, 0.0);\n", 1.0 / width);
        }

        StringBuilder shaderString = new StringBuilder("attribute vec4 position;\n" +
                "   attribute vec2 inputTextureCoordinates;\n" +
                "   varying vec2 blurCoordinates[" + (1 + (numberOfOptimizedOffsets * 2)) + "];\n" +
                "   const vec2 singleStepOffset = " + singleStepOffset +
                "   void main() {\n" +
                position +
                "       blurCoordinates[0] = inputTextureCoordinates;\n");

        for (int currentOptimizedOffset = 0; currentOptimizedOffset < numberOfOptimizedOffsets; currentOptimizedOffset++) {
            shaderString.append(format("blurCoordinates[%d] = inputTextureCoordinates + singleStepOffset * %f;\n", currentOptimizedOffset * 2 + 1, optimizedGaussianOffsets[currentOptimizedOffset]));
            shaderString.append(format("blurCoordinates[%d] = inputTextureCoordinates - singleStepOffset * %f;\n", currentOptimizedOffset * 2 + 2, optimizedGaussianOffsets[currentOptimizedOffset]));
        }

        shaderString.append("}");

        return shaderString.toString();
    }

    private String buildFragmentShader(int blurRadius, float sigma, boolean sourceIsFbo) {
        // First, generate the normal Gaussian weights for a given sigma
        float[] standardGaussianWeights = new float[blurRadius + 1];
        float sumOfWeights = 0f;
        for (int currentGaussianWeightIndex = 0; currentGaussianWeightIndex < blurRadius + 1; currentGaussianWeightIndex++) {
            standardGaussianWeights[currentGaussianWeightIndex] = (float) ((1.0 / sqrt(2.0 * PI * pow(sigma, 2.0))) * exp(-pow(currentGaussianWeightIndex, 2.0) / (2.0 * pow(sigma, 2.0))));

            if (currentGaussianWeightIndex == 0) {
                sumOfWeights += standardGaussianWeights[currentGaussianWeightIndex];
            } else {
                sumOfWeights += 2f * standardGaussianWeights[currentGaussianWeightIndex];
            }
        }

        // Next, normalize these weights to prevent the clipping of the Gaussian curve at the end of the discrete samples from reducing luminance
        for (int currentGaussianWeightIndex = 0; currentGaussianWeightIndex < blurRadius + 1; currentGaussianWeightIndex++) {
            standardGaussianWeights[currentGaussianWeightIndex] = standardGaussianWeights[currentGaussianWeightIndex] / sumOfWeights;
        }

        // From these weights we calculate the offsets to read interpolated values from
        int numberOfOptimizedOffsets = min(blurRadius / 2 + (blurRadius % 2), 7);
        int trueNumberOfOptimizedOffsets = blurRadius / 2 + (blurRadius % 2);

        String source;
        String singleStepOffset;

        if (sourceIsFbo) {
            source = "uniform sampler2D inputImageTexture;\n";
            singleStepOffset = format("vec2(0.0, %f);\n", 1.0 / height);
        } else {
            source = "#extension GL_OES_EGL_image_external : require\n" +
                    "uniform samplerExternalOES inputImageTexture;\n";
            singleStepOffset = format("vec2(%f, 0.0);\n", 1.0 / width);
        }

        StringBuilder shaderString = new StringBuilder(source +
                "   varying highp vec2 blurCoordinates[" + (1 + (numberOfOptimizedOffsets * 2)) + "];\n" +
                "   const highp vec2 singleStepOffset = " + singleStepOffset +
                "   void main() {\n" +
                "       lowp vec3 sum = vec3(0.0);\n");

        shaderString.append(format("sum += texture2D(inputImageTexture, blurCoordinates[0]).rgb * %f;\n", standardGaussianWeights[0]));

        for (int currentBlurCoordinateIndex = 0; currentBlurCoordinateIndex < numberOfOptimizedOffsets; currentBlurCoordinateIndex++) {
            float firstWeight = standardGaussianWeights[currentBlurCoordinateIndex * 2 + 1];
            float secondWeight = standardGaussianWeights[currentBlurCoordinateIndex * 2 + 2];
            float optimizedWeight = firstWeight + secondWeight;

            shaderString.append(format("sum += texture2D(inputImageTexture, blurCoordinates[%d]).rgb * %f;\n", ((currentBlurCoordinateIndex * 2) + 1), optimizedWeight));
            shaderString.append(format("sum += texture2D(inputImageTexture, blurCoordinates[%d]).rgb * %f;\n", ((currentBlurCoordinateIndex * 2) + 2), optimizedWeight));
        }

        // If the number of required samples exceeds the amount we can pass in via varyings, we have to do dependent texture reads in the fragment shader
        if (trueNumberOfOptimizedOffsets > numberOfOptimizedOffsets) {
            for (int currentOverflowTextureRead = numberOfOptimizedOffsets; currentOverflowTextureRead < trueNumberOfOptimizedOffsets; currentOverflowTextureRead++) {
                float firstWeight = standardGaussianWeights[currentOverflowTextureRead * 2 + 1];
                float secondWeight = standardGaussianWeights[currentOverflowTextureRead * 2 + 2];

                float optimizedWeight = firstWeight + secondWeight;
                float optimizedOffset = (firstWeight * (currentOverflowTextureRead * 2 + 1) + secondWeight * (currentOverflowTextureRead * 2 + 2)) / optimizedWeight;

                shaderString.append(format("sum += texture2D(inputImageTexture, blurCoordinates[0] + singleStepOffset * %f).rgb * %f;\n", optimizedOffset, optimizedWeight));
                shaderString.append(format("sum += texture2D(inputImageTexture, blurCoordinates[0] - singleStepOffset * %f).rgb * %f;\n", optimizedOffset, optimizedWeight));
            }
        }

        shaderString.append("gl_FragColor = vec4(sum, 1.0);\n}");

        return shaderString.toString();
    }
}
