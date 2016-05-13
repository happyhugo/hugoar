package com.android.hugoar.VuforiaSamples.base;

import android.content.res.Resources;
import android.opengl.GLES20;
import android.opengl.Matrix;


/**
 * Created by hugo on 5/13/16.
 */
public class Model {

    private MeshInfo meshinfo;
    //主EFFECT PROGRAM
    public int hProgram;
    //UNIFORM
    public int hmxWVP;
    public int hmxView;
    public int hmxWorldView;
    public int hv3LightDir;
    public int hspDiffuse;//采样器
    //ATTRIBUTE
    public int hv3Pos;
    public int hv3Normal;
    public int hv2UV;
    public float xAngle=0;
    public float yAngle=0;

    public Model(Resources resources,String obj){
        initVertexData(resources,obj);
        initShader(resources);
    }

    private void initVertexData(Resources resources,String obj) {
        meshinfo = LoadUtils.loadMesh(resources,obj);
    }

    private void initShader(Resources resources) {
        String textVS = ShaderUtil.loadFromAssetsFile("g9phong.vs", resources);
        String textPS = ShaderUtil.loadFromAssetsFile("g9phong.ps", resources);
        //创建effect句柄
        hProgram = ShaderUtil.createProgram(textVS, textPS);
        //uniform
        hmxWVP = GLES20.glGetUniformLocation(hProgram, "g_mxWVP");
        hmxView = GLES20.glGetUniformLocation(hProgram, "g_mxView");
        hmxWorldView= GLES20.glGetUniformLocation(hProgram, "g_mxWorldView");
        hv3LightDir = GLES20.glGetUniformLocation(hProgram, "g_v3LightDir");
        hspDiffuse = GLES20.glGetUniformLocation(hProgram, "spDiffuse");
        //attribute
        hv3Pos = GLES20.glGetAttribLocation(hProgram, "v3Pos");
        hv3Normal = GLES20.glGetAttribLocation(hProgram, "v3Normal");
        hv2UV  = GLES20.glGetAttribLocation(hProgram, "v2UV");
    }

    public void initCamera(int iWinWidth,int iWinHeight){
        float ratio = (float) iWinWidth / iWinHeight;
        // 调用此方法计算产生透视投影矩阵
        MatrixState.setProjectFrustum(-ratio, ratio, -1, 1, 0.8f, 15);
        // 调用此方法产生摄像机9参数位置矩阵
        MatrixState.setCamera(0, 0, 1.1f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
    }

    public void drawSelf()
    {
        //在Effect类里面渲染 好像有点与理不合，不过确实方便不少
        //空值不检测了 使用时小心
        GLES20.glUseProgram(hProgram);
        //计算矩阵
        float[] mxWVP = new float[16];
        float[] mxWorldView = new float[16];
        float[] mxView = new float[16];
        float[] mMMatrix = new float[16];

        Matrix.setRotateM(mMMatrix, 0, 0, 1, 0, 0);
//        Matrix.scaleM(mMMatrix, 0, .1f, .1f, .1f);
//        Matrix.translateM(mMMatrix, 0, 0, 0, -5);
        Matrix.rotateM(mMMatrix, 0, yAngle, 0, 1, 0);
        Matrix.rotateM(mMMatrix, 0, xAngle, 1, 0, 0);

//      System.out.println("model.GetWorldMatrix():" + java.util.Arrays.toString(model.GetWorldMatrix()));
        mxView = MatrixState.getMVMatrix();
        Matrix.multiplyMM(mxWorldView, 0, mxView, 0, mMMatrix, 0);
        Matrix.multiplyMM(mxWVP, 0, MatrixState.getmProjMatrixMatrix(), 0, mxWorldView, 0);


        //传矩阵
        GLES20.glUniformMatrix4fv(hmxWVP, 1, false, mxWVP, 0);      //总矩阵
        GLES20.glUniformMatrix4fv(hmxView, 1, false, mxView, 0);   //摄像矩阵
        GLES20.glUniformMatrix4fv(hmxWorldView, 1, false, mxWorldView, 0);  //摄像矩阵与变换矩阵
        //传光向量
        float[] v3LightDir = {-0.37139067f, -0.9284767f, -0.0f};
//      lcamera.GetLook(v3LightDir);
        GLES20.glUniform3fv(hv3LightDir, 1, v3LightDir, 0);
        //分SUBSET 传顶点及纹理 并 draw\
        SubInfo curSub = null;
        for(int i=0; i<meshinfo.iSubsets; i++)
        {
            curSub = meshinfo.GetSubset(i);
            //顶点操作
            GLES20.glVertexAttribPointer(hv3Pos,
                    3, GLES20.GL_FLOAT, false,
                    curSub.posInfo._iStride,
                    curSub.posInfo._fbuf);
            GLES20.glVertexAttribPointer(hv3Normal,
                    3, GLES20.GL_FLOAT, false,
                    curSub.normalInfo._iStride,
                    curSub.normalInfo._fbuf);
            GLES20.glVertexAttribPointer(hv2UV,
                    2, GLES20.GL_FLOAT, false,
                    curSub.uvInfo._iStride,
                    curSub.uvInfo._fbuf);
            GLES20.glEnableVertexAttribArray(hv3Pos);
            GLES20.glEnableVertexAttribArray(hv3Normal);
            GLES20.glEnableVertexAttribArray(hv2UV);
            //TEXTURE操作//又中了次枪
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, curSub.idDTex);
            GLES20.glUniform1i(hspDiffuse, 0);


            //绘制当前 subset
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, curSub.iSubVertCount);
        }//end for
    }

    public void drawSelf(float[] modelProject,float[] modelview)
    {
        //在Effect类里面渲染 好像有点与理不合，不过确实方便不少
        //空值不检测了 使用时小心
//        GLES20.glUseProgram(hProgram);
        //计算矩阵
        float[] mxWVP = new float[16];
        float[] mxWorldView = new float[16];
//        float[] mxView = new float[16];
//        float[] mMMatrix = new float[16];

//        Matrix.setRotateM(mMMatrix, 0, 0, 1, 0, 0);
//        Matrix.scaleM(mMMatrix, 0, .1f, .1f, .1f);
//        Matrix.translateM(mMMatrix, 0, 0, 0, -5);
//        Matrix.rotateM(mMMatrix, 0, yAngle, 0, 1, 0);
//        Matrix.rotateM(mMMatrix, 0, xAngle, 1, 0, 0);

//      System.out.println("model.GetWorldMatrix():" + java.util.Arrays.toString(model.GetWorldMatrix()));
        mxWorldView = modelview;
//        Matrix.multiplyMM(mxWorldView, 0, mxView, 0, mMMatrix, 0);
        Matrix.multiplyMM(mxWVP, 0, modelProject, 0, mxWorldView, 0);


        //传矩阵
        GLES20.glUniformMatrix4fv(hmxWVP, 1, false, mxWVP, 0);
//        GLES20.glUniformMatrix4fv(hmxView, 1, false, mxView, 0);
        GLES20.glUniformMatrix4fv(hmxWorldView, 1, false, mxWorldView, 0);
        //传光向量
        float[] v3LightDir = {-0.37139067f, -0.9284767f, -0.0f};
//      lcamera.GetLook(v3LightDir);
        GLES20.glUniform3fv(hv3LightDir, 1, v3LightDir, 0);
        //分SUBSET 传顶点及纹理 并 draw\
        SubInfo curSub = null;
        for(int i=0; i<meshinfo.iSubsets; i++)
        {
            curSub = meshinfo.GetSubset(i);
            //顶点操作
            GLES20.glVertexAttribPointer(hv3Pos,
                    3, GLES20.GL_FLOAT, false,
                    curSub.posInfo._iStride,
                    curSub.posInfo._fbuf);
            GLES20.glVertexAttribPointer(hv3Normal,
                    3, GLES20.GL_FLOAT, false,
                    curSub.normalInfo._iStride,
                    curSub.normalInfo._fbuf);
            GLES20.glVertexAttribPointer(hv2UV,
                    2, GLES20.GL_FLOAT, false,
                    curSub.uvInfo._iStride,
                    curSub.uvInfo._fbuf);
            GLES20.glEnableVertexAttribArray(hv3Pos);
            GLES20.glEnableVertexAttribArray(hv3Normal);
            GLES20.glEnableVertexAttribArray(hv2UV);
            //TEXTURE操作//又中了次枪
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, curSub.idDTex);
            GLES20.glUniform1i(hspDiffuse, 0);


            //绘制当前 subset
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, curSub.iSubVertCount);
        }//end for
    }
}
