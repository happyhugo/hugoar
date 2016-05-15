package com.android.hugoar.VuforiaSamples.font;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

public class FontUtil 
{
	static int cIndex=0;
	static final float textSize=40;
	static int R=255;
	static int G=255;
	static int B=255;
	public static Bitmap generateWLT(String[] str,int width,int height)
	{
		Paint paint=new Paint();
		paint.setARGB(255, R, G, B);
		paint.setTextSize(textSize);
		paint.setTypeface(null);
		paint.setFlags(Paint.ANTI_ALIAS_FLAG);
		Bitmap bmTemp= Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvasTemp = new Canvas(bmTemp);
		for(int i=0;i<str.length;i++)
		{
			canvasTemp.drawText(str[i], 0, textSize*i+(i-1)*5, paint);
		}
		return bmTemp;
	}
	static String[] content=
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
}