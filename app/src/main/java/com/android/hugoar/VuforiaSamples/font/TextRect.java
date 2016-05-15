package com.android.hugoar.VuforiaSamples.font;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.android.hugoar.VuforiaSamples.base.ShaderUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class TextRect
{
	static final float UNIT_SIZE=0.6f;
	int mProgram;//�Զ�����Ⱦ���߳���id
    int muMVPMatrixHandle;//�ܱ任��������id
    int maPositionHandle; //����λ����������id  
    int maTexCoorHandle; //�������������������id  
    
	FloatBuffer mVertexBuffer;//���������ݻ���
	FloatBuffer mTexCoorBuffer;//�������������ݻ���
    int vCount=0;
    int texureId;
    String[] content=
            {
                    "你好的发售发动是非得失氛围",
                    "你好的发售发动是非得失氛围",
                    "你好的发售发动是非得失氛围",
                    "你好的发售发动是非得失氛围",
                    "你好的发售发动是非得失氛围",
                    "你好的发售发动是非得失氛围",
                    "你好的发售发动是非得失氛围",
                    "你好的发售发动是非得失氛围",
                    "你好的发售发动是非得失氛围",
            };
    int wlWidth=512;//����������
    int wlHeight=512;//��������߶�

    public TextRect(Resources mv)
    {
    	initVertexData();
    	initShader(mv);
        texureId = initTexture();
    }

    //��������id
    public int initTexture()
    {
        Bitmap bitmap=FontUtil.generateWLT(content, wlWidth, wlHeight);
        //�������ID
        int[] textures = new int[1];
        GLES20.glGenTextures
                (
                        1,          //���������id������
                        textures,   //����id������
                        0           //ƫ����
                );
        int textureId=textures[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_REPEAT);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_REPEAT);

        //ʵ�ʼ�������
        GLUtils.texImage2D
                (
                        GLES20.GL_TEXTURE_2D,   //�������ͣ���OpenGL ES�б���ΪGL10.GL_TEXTURE_2D
                        0,                      //����Ĳ�Σ�0��ʾ��ͼ��㣬�������Ϊֱ����ͼ
                        bitmap,              //����ͼ��
                        0                      //����߿�ߴ�
                );
        bitmap.recycle(); 		  //������سɹ����ͷ�ͼƬ
        return textureId;
    }

    //��ʼ��������ݵķ���
    public void initVertexData()
    {
    	vCount=6;
        float vertices[]=new float[]
        {
        	-UNIT_SIZE,-UNIT_SIZE,0,
            UNIT_SIZE,-UNIT_SIZE,0,
            UNIT_SIZE,UNIT_SIZE,0,
            
            UNIT_SIZE,UNIT_SIZE,0,
            -UNIT_SIZE,UNIT_SIZE,0,
            -UNIT_SIZE,-UNIT_SIZE,0,
        };
        //�������������ݻ���
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());//�����ֽ�˳��
        mVertexBuffer = vbb.asFloatBuffer();//ת��ΪFloat�ͻ���
        mVertexBuffer.put(vertices);//�򻺳����з��붥��������
        mVertexBuffer.position(0);//���û�������ʼλ��
        
        float[] texcoor=new float[]
        {
        	0,1,   1,1,   1,0,
        	1,0,   0,0,   0,1
        };
        ByteBuffer tbb = ByteBuffer.allocateDirect(texcoor.length * 4);
        tbb.order(ByteOrder.nativeOrder());//�����ֽ�˳��
        mTexCoorBuffer = tbb.asFloatBuffer();//ת��ΪFloat�ͻ���
        mTexCoorBuffer.put(texcoor);//�򻺳����з��붥��������
        mTexCoorBuffer.position(0);//���û�������ʼλ��        
    }
    //��ʼ��shader
    public void initShader(Resources mv)
    {
    	//���ض�����ɫ���Ľű�����
        String mVertexShader= ShaderUtil.loadFromAssetsFile("font_vertex.sh", mv);
        //����ƬԪ��ɫ���Ľű�����
        String mFragmentShader=ShaderUtil.loadFromAssetsFile("font_frag.sh", mv);
        //���ڶ�����ɫ����ƬԪ��ɫ����������
        mProgram = ShaderUtil.createProgram(mVertexShader, mFragmentShader);
        //��ȡ�����ж���λ����������id  
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        //��ȡ�����ж������������������id  
        maTexCoorHandle= GLES20.glGetAttribLocation(mProgram, "aTexCoor");
        //��ȡ�������ܱ任��������id
        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
    }
    //�����Ʒ���
    public void drawSelf(float[] modelview)
    {
    	//ָ��ʹ��ĳ��shader����
   	 	GLES20.glUseProgram(mProgram);
        //�����ձ任������shader����
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, modelview, 0);
        //���Ͷ���λ�����
        GLES20.glVertexAttribPointer
        (
                maPositionHandle,
                3,
                GLES20.GL_FLOAT,
                false,
                3 * 4,
                mVertexBuffer
        );       
        //���Ͷ�������������
        GLES20.glVertexAttribPointer
        (
                maTexCoorHandle,
                2,
                GLES20.GL_FLOAT,
                false,
                2 * 4,
                mTexCoorBuffer
        );   
        //���?��λ�á���������������
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        GLES20.glEnableVertexAttribArray(maTexCoorHandle);
        
        //������
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texureId);
        
        //�����������
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vCount);
    }
}