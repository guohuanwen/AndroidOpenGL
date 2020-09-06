/*
 *  Copyright 2015 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */


package com.bigwen.opengl.gl.ve_gl;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import java.nio.FloatBuffer;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Helper class to draw an opaque quad on the target viewport location. Rotation, mirror, and
 * cropping is specified using a 4x4 texture coordinate transform matrix. The frame input can either
 * be an OES texture or YUV textures in I420 format. The GL state must be preserved between draw
 * calls, this is intentional to maximize performance. The function release() must be called
 * manually to free the resources held by this object.
 */
public class GlRectDrawer {
    // clang-format off
    // Simple vertex shader, used for both YUV and OES.
    private static final String VERTEX_SHADER_STRING =
            "varying vec2 interp_tc;\n"
                    + "attribute vec4 in_pos;\n"
                    + "attribute vec4 in_tc;\n"
                    + "\n"
                    + "uniform mat4 texMatrix;\n"
                    + "\n"
                    + "void main() {\n"
                    + "    gl_Position = in_pos;\n"
                    + "    interp_tc = (texMatrix * in_tc).xy;\n"
                    + "}\n";

    private static final String YUV_FRAGMENT_SHADER_STRING =
            "precision mediump float;\n"
                    + "varying vec2 interp_tc;\n"
                    + "\n"
                    + "uniform sampler2D y_tex;\n"
                    + "uniform sampler2D u_tex;\n"
                    + "uniform sampler2D v_tex;\n"
                    + "\n"
                    + "void main() {\n"
                    // CSC according to http://www.fourcc.org/fccyvrgb.php
                    + "  float y = texture2D(y_tex, interp_tc).r * 1.16438;\n"
                    + "  float u = texture2D(u_tex, interp_tc).r;\n"
                    + "  float v = texture2D(v_tex, interp_tc).r;\n"
                    + "  gl_FragColor = vec4(y + 1.59603 * v - 0.874202, "
                    + "                      y - 0.391762 * u - 0.812968 * v + 0.531668, "
                    + "                      y + 2.01723 * u - 1.08563, 1);\n"
                    + "}\n";

    private static final String RGB_FRAGMENT_SHADER_STRING =
            "precision mediump float;\n"
                    + "varying vec2 interp_tc;\n"
                    + "\n"
                    + "uniform sampler2D rgb_tex;\n"
                    + "\n"
                    + "void main() {\n"
                    + "  gl_FragColor = texture2D(rgb_tex, interp_tc);\n"
                    + "}\n";

    private static final String OES_FRAGMENT_SHADER_STRING =
            "#extension GL_OES_EGL_image_external : require\n"
                    + "precision mediump float;\n"
                    + "varying vec2 interp_tc;\n"
                    + "\n"
                    + "uniform samplerExternalOES oes_tex;\n"
                    + "\n"
                    + "void main() {\n"
                    + "  gl_FragColor = texture2D(oes_tex, interp_tc);\n"
                    + "}\n";

    private static final String OES_FRAGMENT_SHADER_STRING_WITH_BEAUTIFUL =
                    "#extension GL_OES_EGL_image_external : require\n" +
                    "varying highp vec2 interp_tc;\n" +//textureCoordinate 纹理坐标
                    "uniform samplerExternalOES oes_tex;\n" +//inputImageTexture 输入纹理
                    "uniform highp vec2 singleStepOffset;\n" +
                    "uniform highp vec4 params;\n" +
                    "uniform highp float brightness;\n" +
                    "const highp vec3 W = vec3(0.299, 0.587, 0.114);\n" +
                    "const highp mat3 saturateMatrix = mat3(\n" +
                    "        1.1102, -0.0598, -0.061,\n" +
                    "        -0.0774, 1.0826, -0.1186,\n" +
                    "        -0.0228, -0.0228, 1.1772);\n" +
                    "highp vec2 blurCoordinates[24];\n" +
                    "highp float hardLight(highp float color) {\n" +
                    "    if (color <= 0.5)\n" +
                    "        color = color * color * 2.0;\n" +
                    "    else\n" +
                    "        color = 1.0 - ((1.0 - color)*(1.0 - color) * 2.0);\n" +
                    "    return color;\n" +
                    "}\n" +
                    "\n" +
                    "void main(){\n" +
                    "    highp vec3 centralColor = texture2D(oes_tex, interp_tc).rgb;\n" +
                    "    blurCoordinates[0] = interp_tc.xy + singleStepOffset * vec2(0.0, -10.0);\n" +
                    "    blurCoordinates[1] = interp_tc.xy + singleStepOffset * vec2(0.0, 10.0);\n" +
                    "    blurCoordinates[2] = interp_tc.xy + singleStepOffset * vec2(-10.0, 0.0);\n" +
                    "    blurCoordinates[3] = interp_tc.xy + singleStepOffset * vec2(10.0, 0.0);\n" +
                    "    blurCoordinates[4] = interp_tc.xy + singleStepOffset * vec2(5.0, -8.0);\n" +
                    "    blurCoordinates[5] = interp_tc.xy + singleStepOffset * vec2(5.0, 8.0);\n" +
                    "    blurCoordinates[6] = interp_tc.xy + singleStepOffset * vec2(-5.0, 8.0);\n" +
                    "    blurCoordinates[7] = interp_tc.xy + singleStepOffset * vec2(-5.0, -8.0);\n" +
                    "    blurCoordinates[8] = interp_tc.xy + singleStepOffset * vec2(8.0, -5.0);\n" +
                    "    blurCoordinates[9] = interp_tc.xy + singleStepOffset * vec2(8.0, 5.0);\n" +
                    "    blurCoordinates[10] = interp_tc.xy + singleStepOffset * vec2(-8.0, 5.0);\n" +
                    "    blurCoordinates[11] = interp_tc.xy + singleStepOffset * vec2(-8.0, -5.0);\n" +
                    "    blurCoordinates[12] = interp_tc.xy + singleStepOffset * vec2(0.0, -6.0);\n" +
                    "    blurCoordinates[13] = interp_tc.xy + singleStepOffset * vec2(0.0, 6.0);\n" +
                    "    blurCoordinates[14] = interp_tc.xy + singleStepOffset * vec2(6.0, 0.0);\n" +
                    "    blurCoordinates[15] = interp_tc.xy + singleStepOffset * vec2(-6.0, 0.0);\n" +
                    "    blurCoordinates[16] = interp_tc.xy + singleStepOffset * vec2(-4.0, -4.0);\n" +
                    "    blurCoordinates[17] = interp_tc.xy + singleStepOffset * vec2(-4.0, 4.0);\n" +
                    "    blurCoordinates[18] = interp_tc.xy + singleStepOffset * vec2(4.0, -4.0);\n" +
                    "    blurCoordinates[19] = interp_tc.xy + singleStepOffset * vec2(4.0, 4.0);\n" +
                    "    blurCoordinates[20] = interp_tc.xy + singleStepOffset * vec2(-2.0, -2.0);\n" +
                    "    blurCoordinates[21] = interp_tc.xy + singleStepOffset * vec2(-2.0, 2.0);\n" +
                    "    blurCoordinates[22] = interp_tc.xy + singleStepOffset * vec2(2.0, -2.0);\n" +
                    "    blurCoordinates[23] = interp_tc.xy + singleStepOffset * vec2(2.0, 2.0);\n" +
                    "\n" +
                    "    highp float sampleColor = centralColor.g * 22.0;\n" +
                    "    sampleColor += texture2D(oes_tex, blurCoordinates[0]).g;\n" +
                    "    sampleColor += texture2D(oes_tex, blurCoordinates[1]).g;\n" +
                    "    sampleColor += texture2D(oes_tex, blurCoordinates[2]).g;\n" +
                    "    sampleColor += texture2D(oes_tex, blurCoordinates[3]).g;\n" +
                    "    sampleColor += texture2D(oes_tex, blurCoordinates[4]).g;\n" +
                    "    sampleColor += texture2D(oes_tex, blurCoordinates[5]).g;\n" +
                    "    sampleColor += texture2D(oes_tex, blurCoordinates[6]).g;\n" +
                    "    sampleColor += texture2D(oes_tex, blurCoordinates[7]).g;\n" +
                    "    sampleColor += texture2D(oes_tex, blurCoordinates[8]).g;\n" +
                    "    sampleColor += texture2D(oes_tex, blurCoordinates[9]).g;\n" +
                    "    sampleColor += texture2D(oes_tex, blurCoordinates[10]).g;\n" +
                    "    sampleColor += texture2D(oes_tex, blurCoordinates[11]).g;\n" +
                    "    sampleColor += texture2D(oes_tex, blurCoordinates[12]).g * 2.0;\n" +
                    "    sampleColor += texture2D(oes_tex, blurCoordinates[13]).g * 2.0;\n" +
                    "    sampleColor += texture2D(oes_tex, blurCoordinates[14]).g * 2.0;\n" +
                    "    sampleColor += texture2D(oes_tex, blurCoordinates[15]).g * 2.0;\n" +
                    "    sampleColor += texture2D(oes_tex, blurCoordinates[16]).g * 2.0;\n" +
                    "    sampleColor += texture2D(oes_tex, blurCoordinates[17]).g * 2.0;\n" +
                    "    sampleColor += texture2D(oes_tex, blurCoordinates[18]).g * 2.0;\n" +
                    "    sampleColor += texture2D(oes_tex, blurCoordinates[19]).g * 2.0;\n" +
                    "    sampleColor += texture2D(oes_tex, blurCoordinates[20]).g * 3.0;\n" +
                    "    sampleColor += texture2D(oes_tex, blurCoordinates[21]).g * 3.0;\n" +
                    "    sampleColor += texture2D(oes_tex, blurCoordinates[22]).g * 3.0;\n" +
                    "    sampleColor += texture2D(oes_tex, blurCoordinates[23]).g * 3.0;\n" +
                    "\n" +
                    "    sampleColor = sampleColor / 62.0;\n" +
                    "\n" +
                    "    highp float highPass = centralColor.g - sampleColor + 0.5;\n" +
                    "\n" +
                    "    for (int i = 0; i < 5; i++) {\n" +
                    "        highPass = hardLight(highPass);\n" +
                    "    }\n" +
                    "    highp float lumance = dot(centralColor, W);\n" +
                    "\n" +
                    "    highp float alpha = pow(lumance, params.r);\n" +
                    "\n" +
                    "    highp vec3 smoothColor = centralColor + (centralColor-vec3(highPass))*alpha*0.1;\n" +
                    "\n" +
                    "    smoothColor.r = clamp(pow(smoothColor.r, params.g), 0.0, 1.0);\n" +
                    "    smoothColor.g = clamp(pow(smoothColor.g, params.g), 0.0, 1.0);\n" +
                    "    smoothColor.b = clamp(pow(smoothColor.b, params.g), 0.0, 1.0);\n" +
                    "\n" +
                    "    highp vec3 lvse = vec3(1.0)-(vec3(1.0)-smoothColor)*(vec3(1.0)-centralColor);\n" +
                    "    highp vec3 bianliang = max(smoothColor, centralColor);\n" +
                    "    highp vec3 rouguang = 2.0*centralColor*smoothColor + centralColor*centralColor - 2.0*centralColor*centralColor*smoothColor;\n" +
                    "\n" +
                    "    gl_FragColor = vec4(mix(centralColor, lvse, alpha), 1.0);\n" +
                    "    gl_FragColor.rgb = mix(gl_FragColor.rgb, bianliang, alpha);\n" +
                    "    gl_FragColor.rgb = mix(gl_FragColor.rgb, rouguang, params.b);\n" +
                    "\n" +
                    "    highp vec3 satcolor = gl_FragColor.rgb * saturateMatrix;\n" +
                    "    gl_FragColor.rgb = mix(gl_FragColor.rgb, satcolor, params.a);\n" +
                    "    gl_FragColor.rgb = vec3(gl_FragColor.rgb + vec3(brightness));\n" +
                    "}";
    // clang-format on

    // Vertex coordinates in Normalized Device Coordinates, i.e. (-1, -1) is bottom-left and (1, 1) is
    // top-right.
    private static final FloatBuffer FULL_RECTANGLE_BUF = GlUtil.createFloatBuffer(new float[]{
            -1.0f, -1.0f, // Bottom left.
            1.0f, -1.0f, // Bottom right.
            -1.0f, 1.0f, // Top left.
            1.0f, 1.0f, // Top right.
    });

    // Texture coordinates - (0, 0) is bottom-left and (1, 1) is top-right.
    private static final FloatBuffer FULL_RECTANGLE_TEX_BUF = GlUtil.createFloatBuffer(new float[]{
            0.0f, 0.0f, // Bottom left.
            1.0f, 0.0f, // Bottom right.
            0.0f, 1.0f, // Top left.
            1.0f, 1.0f // Top right.
    });

    private static class Shader {
        public final GlShader glShader;
        public final int texMatrixLocation;
        public final int posLocation;
        public final int tcLocation;
        public int tex0Location;
        public int tex1Location;
        public int tex2Location;

        public Shader(String fragmentShader) {
            this.glShader = new GlShader(VERTEX_SHADER_STRING, fragmentShader);
            this.texMatrixLocation = glShader.getUniformLocation("texMatrix");
            this.posLocation = glShader.getAttribLocation("in_pos");
            this.tcLocation = glShader.getAttribLocation("in_tc");

            GLES20.glEnableVertexAttribArray(this.posLocation);
            GLES20.glEnableVertexAttribArray(this.tcLocation);

            tex0Location = tex1Location = tex2Location = 0;
        }
    }

    // The keys are one of the fragments shaders above.
    private final Map<String, Shader> shaders = new IdentityHashMap<String, Shader>();

    /**
     * Draw an OES texture frame with specified texture transformation matrix. Required resources are
     * allocated at the first call to this function.
     */
    public void drawOes(int oesTextureId, float[] texMatrix, int frameWidth, int frameHeight,
                        int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
        prepareShader(OES_FRAGMENT_SHADER_STRING_WITH_BEAUTIFUL, texMatrix);
        setTexelSize(frameWidth, frameHeight);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        // updateTexImage() may be called from another thread in another EGL context, so we need to
        // bind/unbind the texture in each draw call so that GLES understads it's a new texture.
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId);
        drawRectangle(viewportX, viewportY, viewportWidth, viewportHeight);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
    }

    /**
     * Draw a RGB(A) texture frame with specified texture transformation matrix. Required resources
     * are allocated at the first call to this function.
     */
    public void drawRgb(int textureId, float[] texMatrix, int frameWidth, int frameHeight,
                        int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
        prepareShader(RGB_FRAGMENT_SHADER_STRING, texMatrix);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        drawRectangle(viewportX, viewportY, viewportWidth, viewportHeight);
        // Unbind the texture as a precaution.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    /**
     * Draw a YUV frame with specified texture transformation matrix. Required resources are
     * allocated at the first call to this function.
     */
    public void drawYuv(int[] yuvTextures, float[] texMatrix, int frameWidth, int frameHeight,
                        int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
        prepareShader(YUV_FRAGMENT_SHADER_STRING, texMatrix);
        // Bind the textures.
        for (int i = 0; i < 3; ++i) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextures[i]);
        }
        drawRectangle(viewportX, viewportY, viewportWidth, viewportHeight);
        // Unbind the textures as a precaution..
        for (int i = 0; i < 3; ++i) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        }
    }

    private void drawRectangle(int x, int y, int width, int height) {
        // Draw quad.
        GLES20.glViewport(x, y, width, height);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }


    private float toneLevel;
    private float beautyLevel;
    private float brightLevel;

    private int paramsLocation;
    private int brightnessLocation;
    private int singleStepOffsetLocation;

    private void prepareShader(String fragmentShader, float[] texMatrix) {
        final Shader shader;
        if (shaders.containsKey(fragmentShader)) {
            shader = shaders.get(fragmentShader);
        } else {
            // Lazy allocation.
            shader = new Shader(fragmentShader);
            shaders.put(fragmentShader, shader);
            shader.glShader.useProgram();
            if (fragmentShader == YUV_FRAGMENT_SHADER_STRING) {
                shader.tex0Location = shader.glShader.getUniformLocation("y_tex");
                shader.tex1Location = shader.glShader.getUniformLocation("u_tex");
                shader.tex2Location = shader.glShader.getUniformLocation("v_tex");
            } else if (fragmentShader == RGB_FRAGMENT_SHADER_STRING) {
                shader.tex0Location = shader.glShader.getUniformLocation("rgb_tex");
            } else if (fragmentShader == OES_FRAGMENT_SHADER_STRING) {
                shader.tex0Location = shader.glShader.getUniformLocation("oes_tex");
            } else if (fragmentShader == OES_FRAGMENT_SHADER_STRING_WITH_BEAUTIFUL) {
                shader.tex0Location = shader.glShader.getUniformLocation("oes_tex");
                paramsLocation = shader.glShader.getUniformLocation("params");
                brightnessLocation = shader.glShader.getUniformLocation("brightness");
                singleStepOffsetLocation = shader.glShader.getUniformLocation("singleStepOffset");

                toneLevel = 0.47f;
                beautyLevel = 0.42f;
                brightLevel = 0.34f;

                setParams(beautyLevel, toneLevel);
                setBrightLevel(brightLevel);;
            } else {
                throw new IllegalStateException("Unknown fragment shader: " + fragmentShader);
            }
            GlUtil.checkNoGLES2Error("Initialize fragment shader uniform values.");
        }
        shader.glShader.useProgram();
        // Copy the texture transformation matrix over.

        if (shader.tex0Location != 0) {
            GLES20.glUniform1i(shader.tex0Location, 0);
        }

        if (shader.tex1Location != 0) {
            GLES20.glUniform1i(shader.tex1Location, 1);
        }

        if (shader.tex2Location != 0) {
            GLES20.glUniform1i(shader.tex1Location, 2);
        }

        GLES20.glEnableVertexAttribArray(shader.posLocation);
        GLES20.glEnableVertexAttribArray(shader.tcLocation);
        GLES20.glVertexAttribPointer(shader.posLocation, 2, GLES20.GL_FLOAT, false, 0, FULL_RECTANGLE_BUF);
        GLES20.glVertexAttribPointer(shader.tcLocation, 2, GLES20.GL_FLOAT, false, 0, FULL_RECTANGLE_TEX_BUF);
        GLES20.glUniformMatrix4fv(shader.texMatrixLocation, 1, false, texMatrix, 0);
    }

    public void setBeautyLevel(float beautyLevel) {
        this.beautyLevel = beautyLevel;
        setParams(beautyLevel, toneLevel);
    }

    public void setBrightLevel(float brightLevel) {
        this.brightLevel = brightLevel;
        setFloat(brightnessLocation, 0.6f * (-0.5f + brightLevel));
    }

    public void setParams(float beauty, float tone) {
        float[] vector = new float[4];
        vector[0] = 1.0f - 0.6f * beauty;
        vector[1] = 1.0f - 0.3f * beauty;
        vector[2] = 0.1f + 0.3f * tone;
        vector[3] = 0.1f + 0.3f * tone;
        setFloatVec4(paramsLocation, vector);
    }

    private void setTexelSize(final float w, final float h) {
        setFloatVec2(singleStepOffsetLocation, new float[]{2.0f / w, 2.0f / h});
    }

    protected void setFloat(final int location, final float floatValue) {
        GLES20.glUniform1f(location, floatValue);
    }

    protected void setFloatVec4(final int location, final float[] arrayValue) {
        GLES20.glUniform4fv(location, 1, FloatBuffer.wrap(arrayValue));
    }

    protected void setFloatVec2(final int location, final float[] arrayValue) {
        GLES20.glUniform2fv(location, 1, FloatBuffer.wrap(arrayValue));
    }

    /**
     * Release all GLES resources. This needs to be done manually, otherwise the resources are leaked.
     */
    public void release() {
        for (Shader shader : shaders.values()) {
            shader.glShader.release();
        }
        shaders.clear();
    }
}
