package com.bigwen.opengl;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created by bigwen on 2020/8/30.
 */
public class GLUtil {

    private static final String TAG = "GLUtil";

    /**
     *
     * @param shaderType GLES20.GL_VERTEX_SHADER和GLES20.GL_FRAGMENT_SHADER
     * @param shaderSource glsl string
     * @return shader handle
     */
    public static int compileShader(int shaderType, String shaderSource) {
        int shaderHandle = GLES20.glCreateShader(shaderType);
        if (shaderHandle != 0) {
            GLES20.glShaderSource(shaderHandle, shaderSource);
            GLES20.glCompileShader(shaderHandle);
            int[] status = new int[1];
            GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, status, 0);
            if (status[0] == 0) {
                print("Error compile shader:" + GLES20.glGetShaderInfoLog(shaderHandle));
                GLES20.glDeleteShader(shaderHandle);
                shaderHandle = 0;
            }
        }
        if (shaderHandle == 0) {
            print("compile shader fail");
        }
        return shaderHandle;
    }

    public static int createAndLinkProgram(String vertexCode, String fragmentCode) {
        int programHandle = GLES20.glCreateProgram();
        if (programHandle != 0) {
            //编译shader
            int verTexShaderHanlde = compileShader(GLES20.GL_VERTEX_SHADER, vertexCode);
            int fragmentShaderHandle = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentCode);
            //绑定shader和program
            GLES20.glAttachShader(programHandle, verTexShaderHanlde);
            GLES20.glAttachShader(programHandle, fragmentShaderHandle);
            //链接program
            GLES20.glLinkProgram(programHandle);

            int[] status = new int[1];
            //检查program
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, status, 0);
            if (status[0] == 0) {
                print("Error compile shader:" + GLES20.glGetProgramInfoLog(programHandle));
                GLES20.glDeleteProgram(programHandle);
                programHandle = 0;
            }
            if (programHandle == 0) {
                print("program link fail");
            }
        }
        return programHandle;
    }

    private static void print(String msg) {
        Log.i(TAG, msg);
    }

    public static String readAsserts(Context context, String name) {
        InputStream inputStream = null;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            inputStream = context.getResources().getAssets().open(name);
            byte[] buffer = new byte[1024];
            while (inputStream.read(buffer) != -1) {
                stringBuilder.append(new String(buffer));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        String result = stringBuilder.toString();
        print("readAsserts:\n" + result);
        return result;
    }

    public static String readFromAssets(Context context, String fileName) {
        try {
            InputStreamReader inputReader = new InputStreamReader(context.getResources().getAssets().open(fileName));
            BufferedReader bufReader = new BufferedReader(inputReader);
            String line = "";
            StringBuilder result = new StringBuilder();
            while ((line = bufReader.readLine()) != null) {
                result.append(line);
            }
            print("readFromAssets = " + result.toString());
            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static FloatBuffer array2Buffer(float[] array) {
        ByteBuffer bb = ByteBuffer.allocateDirect(array.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer floatBuffer = bb.asFloatBuffer();
        floatBuffer.put(array);
        floatBuffer.position(0);
        return floatBuffer;
    }

    public static FloatBuffer createBuffer(float[] vertexData) {
        FloatBuffer buffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buffer.put(vertexData, 0, vertexData.length).position(0);
        return buffer;
    }

    public static ShortBuffer createBuffer(short[] indexArray) {
        ShortBuffer buffer = ByteBuffer.allocateDirect(indexArray.length * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        buffer.put(indexArray, 0, indexArray.length).position(0);
        return buffer;
    }

    public static void setAttribute(int locHandle, FloatBuffer floatBuffer, int pointSize) {
        floatBuffer.position(0);
        GLES20.glEnableVertexAttribArray(locHandle);
        GLES20.glVertexAttribPointer(locHandle, pointSize, GLES20.GL_FLOAT, false, 0, floatBuffer);
    }

    public static int createTexture() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        //glCheck("texture generate")
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        //glCheck("texture bind")

        GLES20.glTexParameterf(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);

        GLES20.glTexParameterf(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);

        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);

        return textures[0];
    }
}
