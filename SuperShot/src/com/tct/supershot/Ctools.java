package com.tct.supershot;

import info.papdt.pano.processor.ScreenshotComposer;
import info.papdt.pano.support.FastBitmapReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.tct.supershot.model.SSConfig;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceControl;
import android.widget.Toast;


/**
 *  0 耦合工具类
 * @author cheng
 *
 */
public class Ctools
{

    private static final String  szTAG="TCTss";
    private static final boolean bDebug=true;

    public static final String szImagePath=Environment.getExternalStorageDirectory()
            +File. separator+"supershot"+File. separator;
    //public static final String szCapImageName="ss.png";
    public static final String szOutImageName="out.jpg";

    public static void ClearFolder(File folder)
    {
        if(!folder.exists())
        {
            return;
        }

        if(folder.isFile())
        {
            folder.delete();
        }
        else
        {
            for (File f : folder.listFiles())
            {
                ClearFolder(f);
            }
        }
    }

    public static Bitmap GetFloatViewScreenshotBmp(Rect rec)
    {
        int nScreenWidth=SSConfig.getInstance().getnScreenWidth();
        int nScreenHeight=SSConfig.getInstance().getnScreenHeight();
        int nScreenRotation=SSConfig.getInstance().getnScreenRotation();

        Bitmap mScreenBitmap = null;
        if(nScreenWidth<nScreenHeight)
        {
            mScreenBitmap=SurfaceControl.screenshot( nScreenWidth,nScreenHeight );
        }
        else
        {
            //横屏的截图
            mScreenBitmap=SurfaceControl.screenshot( nScreenHeight,nScreenWidth );
            
            Matrix m=new Matrix();
            m.setRotate((4-nScreenRotation)*90);
            Bitmap tmp=Bitmap.createBitmap(mScreenBitmap, 0, 0, nScreenHeight,nScreenWidth, m, false);
            mScreenBitmap.recycle();
            mScreenBitmap=tmp;
        }

        if (mScreenBitmap == null)
        {
            PrintLog("takeScreenShot null !!!");
            return null;
        }

        //裁剪
        int w=rec.right;
        int h=rec.bottom-rec.top;
        Bitmap newbmp =Bitmap.createBitmap(w, h, Config.RGB_565);
        Canvas cv=new Canvas(newbmp);
        cv.drawBitmap(mScreenBitmap, 0, -rec.top, null);
        cv.save(Canvas.ALL_SAVE_FLAG);
        cv.restore();
        mScreenBitmap.recycle();
        mScreenBitmap=newbmp;
        return mScreenBitmap;
    }

    public static Bitmap GetScreenshotBmp()
    {
        int nScreenWidth=SSConfig.getInstance().getnScreenWidth();
        int nScreenHeight=SSConfig.getInstance().getnScreenHeight();
        int nScreenRotation=SSConfig.getInstance().getnScreenRotation();

        Bitmap mScreenBitmap = null;
        if(nScreenWidth<nScreenHeight)
        {
            mScreenBitmap=SurfaceControl.screenshot( nScreenWidth,nScreenHeight );
        }
        else
        {
            //横屏的截图
            mScreenBitmap=SurfaceControl.screenshot( nScreenHeight,nScreenWidth );
            Matrix m=new Matrix();
            m.setRotate((4-nScreenRotation)*90);
            Bitmap tmp=Bitmap.createBitmap(mScreenBitmap, 0, 0, nScreenHeight,nScreenWidth, m, false);
            mScreenBitmap.recycle();
            mScreenBitmap=tmp;
        }

        if (mScreenBitmap == null)
        {
            PrintLog("takeScreenShot null !!!");
            return null;
        }
        return mScreenBitmap;
    }

    /**
     * 获取手机stausbar高度
     * @param con
     * @return
     */
    public static int GetStatusBarH(Context con)
    {
        int result = 0;
        int resourceId = con.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = con.getResources().getDimensionPixelSize(resourceId);
        }
        if(result==0)
        {
            throw new IllegalStateException("error,How could StatusBarHeight be zero");
        }
        return result;
    }

      /**
     * 打印log使用，依赖开关bDebug
     * @param msg  需要打印的log信息
     */
    public static void PrintLog(String msg)
    {
        if(bDebug&&msg!=null)
        {
            Log.i(szTAG, msg);
        }
    }

    /**
     * 篡改图片，给图片增加上下head，暂时没用了
     * @param bmpIn
     * @param nPicIndex   用来做个随机数，没有太大用处
     * @return
     */
    public static Bitmap HackImage(Bitmap bmpIn,int nPicIndex)
    {
        int w=bmpIn.getWidth();
        int h=bmpIn.getHeight();
        Bitmap newbmp =Bitmap.createBitmap(w, h, Config.ARGB_8888);
        Canvas cv=new Canvas(newbmp);
        cv.drawBitmap(bmpIn, 0, 0, null);

        int nValue=nPicIndex%2==0?3:0;
        int nLineWidth=(4+nValue);
        PrintLog("nPicIndex:"+nPicIndex+"/nValue:"+nValue);

        Paint p=new Paint();
        p.setStrokeWidth(nLineWidth);
        p.setColor(Color.WHITE);
        cv.drawLine(0, 0, w, 0, p);
        p.setColor(Color.BLACK);
        cv.drawLine(0, h-nLineWidth/2, w, h-nLineWidth/2, p);

        cv.save(Canvas.ALL_SAVE_FLAG);
        cv.restore();
        return newbmp;
    }

    public static boolean bNoCaptured()
    {
    	return ScreenshotComposer.getInstance().getMergedBitmap() == null;
    }


    /**
     * 打开合成的图片；因为合成的图片地址是固定的，所以就不需要传入图片地址了
     * @param con
     */
    public static void OpenImageInAPPAndFinished(SuperShotActivity activity)
    {
        PrintLog("==>OpenImageInAPP");
        final SuperShotActivity act=(SuperShotActivity)activity;
        act.getFloatView().post(new Runnable()
        {

            @Override
            public void run()
            {
                act.setInfo(act.getString(R.string.openpic));
            }
        });

        File fOut=new File(szImagePath + szOutImageName);
        if(!fOut.exists())
        {
        	android.util.Log.e("==MyTest==", "OpenImageInAPPAndFinished()# image not found");
        	activity.getHandler().post(new Runnable(){
				@Override
				public void run() {
					Toast.makeText(act, R.string.composefailed, Toast.LENGTH_SHORT).show();
				}});
        }
        else
        {
            try
            {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(fOut), "image/*");
                act.startActivity(intent);
            }
            catch (Exception e)
            {
                Toast.makeText(act, act.getString(R.string.openpicfailed)+e, Toast.LENGTH_SHORT).show();
                PrintLog(act.getString(R.string.openpicfailed)+e);
            }
        }
        act.finish();
    }

    /**
     * add this method in PhoneWindowManager，用于home键启动Supershot应用
     * @param con
     */
    @SuppressWarnings("unused")
    private void StartSupershot(Context con)
    {
        try
        {
            Intent intent = new Intent();
            intent.setClassName("com.TCT.supershot", "com.TCT.supershot.MainActivity");
            intent.setAction(Intent.ACTION_MAIN);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            con.startActivity(intent);
        }
        catch (Exception e)
        {
            Log.i("cheng", "StartSupershot:"+e);
            e.printStackTrace();
        }
    }


    public static boolean SaveImageToLocal(Bitmap bmp,File file)
    {
        try
        {
            FileOutputStream out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();
            Ctools.PrintLog("SaveImageToLocal succeed !");
            return true;
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            Ctools.PrintLog("SaveImageToLocal:"+e);
        }
        catch (IOException e) {
            e.printStackTrace();
            Ctools.PrintLog("SaveImageToLocal:"+e);
        }
        return false;
    }


   /**
    * 图片比对
    * @param bmpCurrentScreen
    * @param bmpForJudgeBottom
    * @return
    */
    public static boolean bIsBottomByCapture(Bitmap bmpCurrentScreen,Bitmap bmpForJudgeBottom)
    {

        FastBitmapReader lastBmp = new FastBitmapReader(bmpCurrentScreen,true);
        FastBitmapReader last2Bmp = new FastBitmapReader(bmpForJudgeBottom,true);

        PrintLog("------> bIsBottom "+lastBmp.getHeight()+" / "+last2Bmp.getHeight());
        //高度不一致
        if (lastBmp.getHeight() != last2Bmp.getHeight())
        {
            PrintLog("------> bIsBottom , compare: hight not same");
            lastBmp.recycle();
            last2Bmp.recycle();
            lastBmp=null;
            last2Bmp=null;
            return false;
        }

        //从开头开始查找不同
        int start = -1;
        int nHeight=lastBmp.getHeight();
        for (int j =  1; j < nHeight/2; j++) {//没有必要全部，比较上半截就可以判断是否移动了
            if (ScreenshotComposer.compareLines(lastBmp, last2Bmp, j)) {
                start = j;
                break;
            }
        }
        lastBmp.recycle();
        last2Bmp.recycle();
        lastBmp=null;
        last2Bmp=null;

        if (start == -1) {
            PrintLog("------> bIsBottom , compare: ===this is bottom");
            return true;
        }
        else
        {
            PrintLog("------> bIsBottom , compare: ===find start is :"+start);
            return false;
        }

    }

}
