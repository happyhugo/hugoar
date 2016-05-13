/*
 * һЩ�����õ��㷨�����ģ�GL20̫�׳��ˣ���normalize vector�ķ�����û��
 * */

package com.android.hugoar.VuforiaSamples.base;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

/**
 * ����: �㷨֧��,GLES����Щ������㷨��û���ṩ,���˺ܱ���
 * @author ����
 * mail: dana9919@163.com
 * QQ 61092517
 */
public final class G9Function {
	/**
	 * ���������Ĺ�񻯣�ע��ֻ֧��float[3]�����ڲ��ӳ��쳣 û�ӵ��ⲿȥ�� ����DEBUGʱҪע��Ӷϵ�
	 * */
	public static void glV3Normalize(float[] vecInOut){
		int iSize =vecInOut.length; 
		if(iSize !=3)
		{
			throw new RuntimeException("err in glV3Normalize");//
		}
		float fScale = (float) Math.sqrt(vecInOut[0] * vecInOut[0] + vecInOut[1] * vecInOut[1] + vecInOut[2] * vecInOut[2]);
		if(fScale == 0)
			return;
		fScale = 1/fScale;//����,��᲻��ܷ�ʱ?
		for(int i=0;i<iSize; i++)
		{
			vecInOut[i] *= fScale;
		}
		return;		
	
	}//ef
	/**
	 * �����,����ǰ������ע��normalize(),ֻ֧��3Ԫ����
	 * */
	public static void glV3Cross(float[] v3OUT,float[] v3A,float[] v3B){
		//������
		if((v3OUT.length !=3)||(v3A.length !=3)||v3B.length!=3){

			throw new RuntimeException("err in glV3Cross");
		}
		/*
		v3OUT[0] =  v3B[1]*v3A[2]-v3A[1]*v3B[2];
		v3OUT[1] =  v3B[2]*v3A[0]-v3A[2]*v3B[0];
		v3OUT[2] =  v3B[0]*v3A[1]-v3A[0]*v3B[1];
		*/
		v3OUT[0] =  v3A[1]*v3B[2]-v3B[1]*v3A[2];
		v3OUT[1] =  v3A[2]*v3B[0]-v3B[2]*v3A[0];
		v3OUT[2] =  v3A[0]*v3B[1]-v3B[0]*v3A[1];
		return;
	}//ef
	/**
	 * �����,����ǰע��normalize() ֻ֧��3Ԫ����
	 */
	public static float glV3Dot(float[] v3A,float[] v3B){
		if((v3A.length !=3)||v3B.length!=3){
			
			throw new RuntimeException("err in glV3Dot");
		}
		return v3A[0]*v3B[0] + v3A[1]*v3B[1] + v3A[2]*v3B[2];
		
	}//ef
	/**
	 * ��Ԫ������� (�������normalize���������,�� v3Dst�Ǻ������������м��ֵ)
	 * @param v3Dst ���
	 * @param v3LParam ���
	 * @param v3RParam �Ҳ�
	 */
	public static void glV3Add(float[] v3Dst,float[] v3LParam,float[] v3RParam){
		if((v3Dst.length !=3 )||(v3LParam.length != 3)|| (v3RParam.length!=3))
			throw new RuntimeException("err in glV3Add");
		for(int i=0; i<3; i++){
			v3Dst[i] = v3LParam[i]+v3RParam[i];
		}
	}//ef
	/**
	 * ��Ԫ��(λ��v3Position)���
	 * 		ע��,��Ϊλ����Ϣ�Ĳ���Ӧ�ù��
	 * @param v3Dst ���,��v3R�㵽v3L�������
	 * @param v3LParam ���
	 * @param v3RParam �Ҳ�
	 */
	public static void glV3Subtract(float[] v3Dst,float[] v3LParam,float[] v3RParam){
		if((v3Dst.length !=3 )||(v3LParam.length != 3)|| (v3RParam.length!=3))
			throw new RuntimeException("err in glV3Add");
		for(int i=0; i<3; i++){
			v3Dst[i] = v3LParam[i]-v3RParam[i];
		}
	}//ef
	/**
	 * 3Ԫ�����任��mxXform�Ǳ任���� ע���ı�����3Ԫ��!! 
	 * */
	public static void glV3Transform(float[] v3InOut,float[] mxXform)
	{
		if((v3InOut.length !=3)||(mxXform.length !=16))
			throw new RuntimeException("err in glV3Transform");
		float f0,f1,f2;
		/*
		f0 = v3InOut[0]*mxXform[0] + v3InOut[1]*mxXform[1] + 
				v3InOut[2]*mxXform[2]+v3InOut[3]*mxXform[3];
		f1 = v3InOut[0]*mxXform[4] + v3InOut[1]*mxXform[5] +
				v3InOut[2]*mxXform[6]+v3InOut[3]*mxXform[7];
		f2 = v3InOut[0]*mxXform[8] + v3InOut[1]*mxXform[9] + 
				v3InOut[2]*mxXform[10]+v3InOut[3]*mxXform[11];
		*/
		f0 = v3InOut[0]*mxXform[0] + v3InOut[1]*mxXform[4] +v3InOut[2]*mxXform[8] + 1*mxXform[12]; 
				
		f1 =  v3InOut[0]*mxXform[1] + v3InOut[1]*mxXform[5] +v3InOut[2]*mxXform[9] + 1*mxXform[13];
		
		f2 =  v3InOut[0]*mxXform[2] + v3InOut[1]*mxXform[6] +v3InOut[2]*mxXform[10] + 1*mxXform[14];
		v3InOut[0] = f0;
		v3InOut[1] = f1;
		v3InOut[2] = f2;
		return;
		
	}//ef
	/**
	 * 4Ԫ�����任��mxXform�Ǳ任���� ע���ı�����3Ԫ��
	 * */
	public static void glV4Transform(float[] v4InOut,float[] mxXform)
	{
		if((v4InOut.length !=4)||(mxXform.length !=16))
			throw new RuntimeException("err in glV3Transform");
		float f0,f1,f2,f3;
		f0 = v4InOut[0]*mxXform[0] + v4InOut[1]*mxXform[1] + 
				v4InOut[2]*mxXform[2]+v4InOut[3]*mxXform[3];
		f1 = v4InOut[0]*mxXform[4] + v4InOut[1]*mxXform[5] +
				v4InOut[2]*mxXform[6]+v4InOut[3]*mxXform[7];
		f2 = v4InOut[0]*mxXform[8] + v4InOut[1]*mxXform[9] + 
				v4InOut[2]*mxXform[10]+v4InOut[3]*mxXform[11];
		f3 = v4InOut[0]*mxXform[12] + v4InOut[1]*mxXform[13] + 
				v4InOut[2]*mxXform[14]+v4InOut[3]*mxXform[15];
		v4InOut[0] = f0;
		v4InOut[1] = f1;
		v4InOut[2] = f2;
		v4InOut[3] = f3;
		return;
		
	}//ef
	/**	 
	 * ��������ת  mxOutΪ��������  fAngleΪ�Ƕ�, v3axis����,������
	 * ����ͨ��,����D3D���ͬ�ۺ���ȽϺ�,�������ľ�������ȫ�෴��,Ӧ���������ֲ�ͬ��ɵ�
	 *��GL�����ת����ȽϺ�һ��
	 */
	public static void glRotateAxis(float[] mxOut,float fAngle,float[] v3Axis){
		//�ȼ��		
		if((mxOut.length !=16)||(v3Axis.length!=3))
		{

			throw new RuntimeException("err in glRotateAxis");
		}
		//���ı�v3Axis ����һ����ȥ���
		float[] v3a = glV3Copy(v3Axis);
		glV3Normalize(v3a);
		// ux v3a[0] ;uy = v3a[1] ; uz = v3a[2]; 
		float s = (float) Math.sin(Math.toRadians(fAngle));//ת��sinֵ
		float c = (float) Math.cos(Math.toRadians(fAngle));//ת��cosֵ
		//���ǰ�����ľ����Ƶ����� 
	
		mxOut[0] = c+(1-c)*v3a[0]*v3a[0];
		mxOut[1] = (1-c)*v3a[0]*v3a[1] + s*v3a[2];
		mxOut[2] = (1-c)*v3a[0]*v3a[2] -s*v3a[1];
		mxOut[3] = 0;
		
		mxOut[4] = (1-c)*v3a[1]*v3a[0] - s*v3a[2];
		mxOut[5] = c+(1-c)*v3a[1]*v3a[1];
		mxOut[6] = (1-c)*v3a[1]*v3a[2]+s*v3a[0];
		mxOut[7] = 0;
		
		mxOut[8] = (1-c)*v3a[2]*v3a[0] + s*v3a[1];
		mxOut[9] = (1-c)*v3a[2]*v3a[1] - s*v3a[0];
		mxOut[10] = c+(1-c)*v3a[2]*v3a[2];
		mxOut[11] = 0;
		
		mxOut[12] = 0;
		mxOut[13] = 0;
		mxOut[14] = 0;
		mxOut[15] = 1.0f;
		
		
	}//ef
	/**
	 * �ڲ�����]] ����һ��float[3]�� vector
	 * ע����� 
	 */
	public static float[] glV3Copy(float[] v3Source){
		
		if(v3Source.length!=3)
		{
			throw new RuntimeException("err in glVec3Copy");
		}
		float[] v3Out = new float[3];
		for(int i=0; i<3; i++){
			v3Out[i] = v3Source[i];
		}
		return v3Out;
	}//ef
	/**
	 * float���鿼��,��src ��dst
	 * @param arrDST Ŀ�� 
	 * @param arrSRC Դ
	 * @return ���鳤��
	 */
	public static int glFArrayCopy(float[] arrDST,float[] arrSRC){
		int iSize = arrDST.length;
		if(arrSRC.length != iSize)
			throw new RuntimeException("err in glFArrayCopy");
		for(int i=0 ; i< iSize; i++){
			arrDST[i] = arrSRC[i];
		}
		return iSize;
	}//ef
	/**
	 * ��һ��FLOAT����ѹ��һ��float buffer ������GLҪ��float buffer ����ϵͳ�ڴ���
	 * ��allocateDirect()�����˺���
	 * @param data Ҫѹ��buffer�����
	 * @return ��Щ�������� floatbuffer
	 */
	public static FloatBuffer PushIntoFLoatBuffer(float[] data){
		FloatBuffer fb = null;
		ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
		bb.order(ByteOrder.nativeOrder());
		fb = bb.asFloatBuffer();
		fb.put(data);
		fb.position(0);
		return fb;
	}//ef
	
	public static FloatBuffer PushIntoFLoatBuffer(ArrayList<Float> arrData){
		//Ҫ������ ���飬��ѹ��BUFFER��ȥ ���Ի����һ���޾����м�����
		int iSize = arrData.size();
		float[] data = new float[iSize];
		for(int i=0; i<iSize; i++){
			data[i] = arrData.get(i);
		}
		
		FloatBuffer fb = null;
		ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
		bb.order(ByteOrder.nativeOrder());
		fb = bb.asFloatBuffer();
		fb.put(data);
		fb.position(0);
		return fb;
	}//ef
	/**
	 * ��һ��INTѹ��INT BUFFER��ʹ�� allocateDirect() �� nativeOrder��BUFFER����ϵͳ�ڴ�
	 * ��Ϊ��GLESֻ��ʶϵͳnative���ڴ�
	 */
	public static IntBuffer PushIntoIntBuffer(int[] data){
		IntBuffer ib = null;
		ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);//4 = Integer.SIZE/8
		bb.order(ByteOrder.nativeOrder());
		ib = bb.asIntBuffer();
		ib.put(data);
		ib.position(0);
		return ib;		
	}//ef
	/**
	 * ͨ����ԴͼƬ�������� ��������id
	 * @param pthis �ݶ�Ϊactivity��context
	 * @param IdDrawable ͼƬ��id ������drawable�ڣ�
	 * @return ����ID
	 */
	public static int CreateTexture(Resources resource,int IdDrawable){
		int[] arrTexID = new int[1];
		GLES20.glGenTextures(1, arrTexID, 0);
		int IdTexOut = arrTexID[0];
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, IdTexOut);//��ID�󶨵�һ������(��Ϊ��)
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		//����ȥͨ����������ͼƬ  ����ע�ⲻһ��Ҫ stream Ҳ����ֱ�� decodeResource ���ļ� 
		InputStream is = resource.openRawResource(IdDrawable);
		
		Bitmap bitmapTemp = null;
		try{
			bitmapTemp = BitmapFactory.decodeStream(is);
		}
		catch (Exception e) {
			// 
			e.printStackTrace();
		}
		finally{
			try {
				is.close();
			} catch (IOException e) {
				Log.e("ERR", e.toString());
			}
		}
		//��ʵ�ʵؼ���ͼƬ
		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmapTemp, 0);
		//����ע�� ����Ǹɵ�bitmapTemp �Ѿ����Դ��ˣ��ٴμ���ֻҪid���У�����bitmaptempû����
		bitmapTemp.recycle();
		//����
		return IdTexOut;
		
	}//ef
	/**
	 * ͨ����ԴͼƬ�������� ��������id
	 * @param resource ϵͳ��Դ
	 * @param strTexFileName �ļ���(�ߴ粻���������˰�,�������ϵͳ assets����)
	 * @return ����ID
	 */
	public static int CreateTexture(Resources resource, String strTexFileName)   {
		// ����Դassets�м������� 
		int[] arrTexID = new int[1];
		GLES20.glGenTextures(1, arrTexID, 0);
		
		int IdTexOut = arrTexID[0];		
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, IdTexOut);//��ID�󶨵�һ������(��Ϊ��)
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		//��ASSETS˽���ļ��ж�ȡ
		Bitmap bitmapTemp = null;
		InputStream ins = null;
		
		
		try {
			ins = resource.getAssets().open(strTexFileName);
			bitmapTemp = BitmapFactory.decodeStream(ins);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			try {
				ins.close();
			} catch (IOException e) {
				// 
				e.printStackTrace();
			}
		}
		
		
		if(bitmapTemp == null)
			return -1;
		
		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmapTemp, 0);//TNN�����ֲ�����Ȼû��THROW��,GL����ƨ
		bitmapTemp.recycle();
		
		int iERR = GLES20.glGetError();
		if(iERR !=0)
			Log.v("", "");
		
		return IdTexOut;
	}//ef
	
}//EC
