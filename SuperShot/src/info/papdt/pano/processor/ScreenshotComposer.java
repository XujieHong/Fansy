package info.papdt.pano.processor;

import static info.papdt.pano.support.Utility.arrayHeadTailMatch;
import info.papdt.pano.support.FastBitmapReader;
import info.papdt.pano.support.FastBitmapWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.util.Log;

import com.tct.supershot.Ctools;

/*
 * To compose a list of screenshots into one
 * But preserving the areas where they are no difference
 *
 */
public class ScreenshotComposer
{
    public static final  boolean DEBUG=true;
    private static final String TAG = ScreenshotComposer.class.getSimpleName();
    //private static final String SEPERATOR = ",";
    //private FastBitmapWriter mLastBitmapWriter = null;
    private Bitmap mMergedBitmap = null;

    static class Region {
        private int startLine;
        private int endLine;

        public Region(){}

        public Region(int startLine, int endLine) {
            this.startLine = startLine;
            this.endLine = endLine;
        }


        @Override
        public String toString()
        {
            return "   Region [startLine=" + startLine + ", endLine=" + endLine
                    + "]";
        }

    }

    public static interface ProgressListener {
        void onAnalyzingImage(int i, int j, int total);
        void onComposingImage();
    }

    private static ScreenshotComposer sInstance;

    //private String mOutDir = "/sdcard/Pictures/Panoramic";

    /**
     * 比较阀值，大于0小于1
     */
    private static float mThreshold = 0.3f;
    /**
     * 状态栏的高度，有时候状态栏有动态图标，导致图片顶部一直在变化，这里跳过状态栏的高度不检测
     */
    private int mStatusBarHeight = 0;//40; // px
    /**
     * 部分窗口边界有阴影，影响识别比对，通过这个这个值来跳过
     */
    private int mShadowHeight = 0;//10; // px

    public static final ScreenshotComposer getInstance() {
        if (sInstance == null) {
            sInstance = new ScreenshotComposer();
        }

        return sInstance;
    }

    private ScreenshotComposer() {
    }
    /*
    public void setOutputDir(String opt) {
        mOutDir = opt;

        File out = new File(mOutDir);
        if (!out.exists()) {
            out.mkdirs();
            out.mkdir();
        }
    }
    */
    public void setThreshold(float threshold) {
        if (threshold > 1 || threshold < 0)
            throw new IllegalArgumentException("illegal threshold");

        mThreshold = threshold;
    }

    public void setStatusBarHeight(int height) {
        mStatusBarHeight = height;
    }

    public void setShadowHeight(int height) {
        mShadowHeight = height;

        if (DEBUG) {
            Log.d(TAG, "shadow height " + mShadowHeight);
        }
    }

    public String compose(String[] images, ProgressListener listener) {
        File[] files = new File[images.length];

        for (int i = 0; i < images.length; i++) {
            files[i] = new File(images[i]);

            if (DEBUG) {
                Log.d(TAG, "Adding " + images[i]);
            }

            if (!files[i].exists()) {

                if (DEBUG) {
                    Log.d(TAG, images[i] + " not exist");
                }

                return null;
            }
        }

        return compose(files, listener);
    }

    private int[] findStartAndEnd(FastBitmapReader currentBmp,FastBitmapReader nextBmp)
    {
        Ctools.PrintLog("currentBmp height:"+currentBmp.getHeight()+"  nextBmp height"+nextBmp.getHeight());

        // 查找起始
        int start = -1;
        int nStop=currentBmp.getHeight()>nextBmp.getHeight()?nextBmp.getHeight():currentBmp.getHeight();
        for (int j = mStatusBarHeight + 1; j < nStop; j++)
        {
            if (compareLines(currentBmp, nextBmp, j))
            {
                start = j;
                break;
            }
        }
        if (start == -1)
        {
            start=0;
            Ctools.PrintLog(szError);
            //return null;
        }

        // 查找截至
        int end = -1;
        nStop=currentBmp.getHeight()>nextBmp.getHeight()?(currentBmp.getHeight()-nextBmp.getHeight()):start;
        nStop=nStop<start?start:nStop;
        Ctools.PrintLog("try find end, nStop is :"+nStop+"  nLinea:"+(currentBmp.getHeight() - 1));
        for (int nLinea = currentBmp.getHeight() - 1; nLinea > nStop; nLinea--)
        {
            int nLineb=nextBmp.getHeight()-(currentBmp.getHeight()-nLinea);
            //Ctools.PrintLog("nLinea:"+nLinea+"  nLineb:"+nLineb);
            if (compareLinesFromBottom(currentBmp, nextBmp, nLinea,nLineb))
            {
                end = nLinea;
                break;
            }
        }
        if (end == -1)
        {
            end = currentBmp.getHeight() - 1;
            Ctools.PrintLog(szError);
            //return null;
        }

        //Log.d(TAG, "head match time " + (System.currentTimeMillis() - headStartTime));

        return new int[]{start,end};
    }

    private int[] FindCommonSpace(int i,FastBitmapReader currentBmp,FastBitmapReader nextBmp,int nStartA,int nEndA)
    {
        //long regionStartTime = System.currentTimeMillis();
        int tmpStartA=nStartA,tmpEndA=nEndA,tmpStartB=nStartA,tmpEndB=nEndA;
        int nSpace=currentBmp.getHeight()-nextBmp.getHeight();
        //保证两张图片对比部分长度相等
        if(nSpace>0)//a比b长
        {
            tmpStartA=nStartA+nSpace;
            tmpEndA=nEndA;
            tmpStartB=nStartA;
            tmpEndB=nEndA-nSpace;
        }
        else//b比a长
        {
            //no need to do anything
        }
        Ctools.PrintLog("curheight:"+currentBmp.getHeight()+"   hashCurrent:"+tmpStartA+"/"+tmpEndA);
        Ctools.PrintLog("nextheight:"+nextBmp.getHeight()+"      hashNext:"+tmpStartB+"/"+tmpEndB);
        Integer[] hashCurrent = buildHashOfRegion(currentBmp, tmpStartA + mShadowHeight, tmpEndA);
        Integer[] hashNext    = buildHashOfRegion(nextBmp   , tmpStartB + mShadowHeight, tmpEndB);
        //Log.d(TAG, "region build time " + (System.currentTimeMillis() - regionStartTime));

        int length = tmpEndA - tmpStartA - mShadowHeight-1;
        int matchLength = arrayHeadTailMatch(hashNext, hashCurrent, length, mThreshold);
        Ctools.PrintLog("length---->"+length+"          matchLength-->"+matchLength);
        if(length==matchLength)
        {
            Log.d(TAG, "======>>>>>arrayHeadTailMatch  same !!<<<<<<=========== ");
            matchLength=matchLength>0?matchLength:0;
        }
        tmpStartB = tmpStartB + matchLength + mShadowHeight+1;
        tmpEndB=nextBmp.getHeight()-(currentBmp.getHeight()-nEndA);
        Log.d(TAG, "Different region between " + i + " and " + (i + 1)+":====matchLength:"+matchLength);
        return new int[]{tmpStartB,tmpEndB};
    }

    //[FR 857010]TCT.NB junqiang.shi 2015.11.23 ADD Begin
    //与FindCommonSpace基本相同，用于TCTcompose2函数中查找两张图片的相同部分
    public int[] FindCommonSpace1(FastBitmapReader currentBmp,FastBitmapReader nextBmp,int nStartA,int nEndA)
    {
        //long regionStartTime = System.currentTimeMillis();
        int tmpStartA=nStartA,tmpEndA=nEndA,tmpStartB=nStartA,tmpEndB=nEndA;
        int nSpace=currentBmp.getHeight()-nextBmp.getHeight();
        android.util.Log.i("==MyTest==","nSpace = " + nSpace + ",currentBmp.getHeight()" + currentBmp.getHeight() + "nextBmp.getHeight() = " + nextBmp.getHeight());
        //保证两张图片对比部分长度相等
        if(nSpace>0)//a比b长
        {
            tmpStartA=nStartA+nSpace;
            tmpEndA=nEndA;
            tmpStartB=nStartA;
            tmpEndB=nEndA-nSpace;
        }
        else//b比a长
        {
            //no need to do anything
        }
        
        if (tmpStartA >= tmpEndA) {
			return new int[]{tmpStartB,tmpEndB};
		}
        Ctools.PrintLog("curheight:"+currentBmp.getHeight()+"   hashCurrent:"+tmpStartA+"/"+tmpEndA);
        Ctools.PrintLog("nextheight:"+nextBmp.getHeight()+"      hashNext:"+tmpStartB+"/"+tmpEndB);
        android.util.Log.i("==MyTest==","tmpStartA = " + tmpStartA + ",tmpEndA = " + tmpEndA + ",tmpStartB = " + tmpStartB + ",tmpEndB = " + tmpEndB );
        Integer[] hashCurrent = buildHashOfRegion(currentBmp, tmpStartA + mShadowHeight, tmpEndA);
        Integer[] hashNext    = buildHashOfRegion(nextBmp   , tmpStartB + mShadowHeight, tmpEndB);
        //Log.d(TAG, "region build time " + (System.currentTimeMillis() - regionStartTime));

        int length = tmpEndA - tmpStartA - mShadowHeight-1;
        int matchLength = arrayHeadTailMatch(hashNext, hashCurrent, length, mThreshold);
        Ctools.PrintLog("length---->"+length+"          matchLength-->"+matchLength);
        if(length==matchLength)
        {
            Log.d(TAG, "======>>>>>arrayHeadTailMatch  same !!<<<<<<=========== ");
            matchLength=matchLength>0?matchLength:0;
        }
        tmpStartB = tmpStartB + matchLength + mShadowHeight+1;
        tmpEndB=nextBmp.getHeight()-(currentBmp.getHeight()-nEndA);
        return new int[]{tmpStartB,tmpEndB};
    }
    //[FR 857010]TCT.NB junqiang.shi 2015.11.23 ADD End

    //[FR 857010]TCT.NB junqiang.shi 2015.11.23 ADD Begin
    //在FindCommonSpace的基础上考虑了nLenPaddingbottom和nlenPaddingTop的影响。用于在TCTcompose1函数中对比两张图片的相同部分。
    private int[] FindCommonSpaceOptimize(FastBitmapReader currentBmp,FastBitmapReader nextBmp,int nStartA,int nEndA,int nLenPaddingbottom,int nlenPaddingTop)
    {
        //long regionStartTime = System.currentTimeMillis();
        int tmpStartA=nStartA,tmpEndA=nEndA,tmpStartB=nStartA,tmpEndB=nEndA;
        int nSpace=currentBmp.getHeight()-nextBmp.getHeight();
        //保证两张图片对比部分长度相等,位置相当
        if(nSpace>0)//a比b长
        {
            tmpStartA=nStartA+nSpace+nlenPaddingTop-nLenPaddingbottom;
            tmpEndA=nEndA-nLenPaddingbottom;
            tmpStartB=nStartA+nlenPaddingTop;
            tmpEndB=nEndA-nSpace;
        }
        else//b比a长
        {
            tmpStartB += nlenPaddingTop;
            tmpEndB += nlenPaddingTop;
        }

        Ctools.PrintLog("curheight:"+currentBmp.getHeight()+"   hashCurrent:"+tmpStartA+"/"+tmpEndA);
        Ctools.PrintLog("nextheight:"+nextBmp.getHeight()+"      hashNext:"+tmpStartB+"/"+tmpEndB);
        Ctools.PrintLog("mShadowHeight ----------------->" + mShadowHeight);
        Integer[] hashCurrent = buildHashOfRegion(currentBmp, tmpStartA + mShadowHeight, tmpEndA);
        Integer[] hashNext    = buildHashOfRegion(nextBmp   , tmpStartB + mShadowHeight, tmpEndB);
        //Log.d(TAG, "region build time " + (System.currentTimeMillis() - regionStartTime));

        int length = tmpEndA - tmpStartA - mShadowHeight-1;
        int matchLength = arrayHeadTailMatch(hashNext, hashCurrent, length, mThreshold);
        Ctools.PrintLog("length---->"+length+"          matchLength-->"+matchLength);
        if(length==matchLength)
        {
            Log.d(TAG, "======>>>>>arrayHeadTailMatch  same !!<<<<<<=========== ");
            matchLength=matchLength>0?matchLength:0;
        }
        tmpStartB = tmpStartB + matchLength + mShadowHeight+1;
        tmpEndB=nextBmp.getHeight()-(currentBmp.getHeight()-nEndA);
        Ctools.PrintLog( "-----------tmpStartA = " + tmpStartA);
        Ctools.PrintLog( "-----------tmpEndA = " + tmpEndA);
        Ctools.PrintLog( "-----------tmpStartB = " + tmpStartB);
        Ctools.PrintLog( "-----------tmpEndB = " + tmpEndB);
        return new int[]{tmpStartA,tmpEndA,tmpStartB,tmpEndB};
    }
    //[FR 857010]TCT.NB junqiang.shi 2015.11.23 ADD Begin

    private int[] FindCommonSpaceForLen(int i,FastBitmapReader currentBmp,FastBitmapReader nextBmp,int nStartA,int nEndA,int len)
    {
        //long regionStartTime = System.currentTimeMillis();
        int tmpStartA=nStartA,tmpEndA=nEndA,tmpStartB=nStartA,tmpEndB=nEndA;
        int nSpace=currentBmp.getHeight()-nextBmp.getHeight();
        if(nSpace>0)//a比b长
        {
            tmpStartA=nStartA+nSpace;
            tmpEndA=nEndA;
            tmpStartB=nStartA;
            tmpEndB=nEndA-nSpace;
        }
        else//b比a长
        {
            //no need to do anything
        }
        //Ctools.PrintLog("curheight:"+currentBmp.getHeight()+"   hashCurrent:"+tmpStartA+"/"+tmpEndA);
        //Ctools.PrintLog("nextheight:"+nextBmp.getHeight()+"      hashNext:"+tmpStartB+"/"+tmpEndB);
        //Integer[] hashCurrent = buildHashOfRegion(currentBmp, tmpStartA + mShadowHeight, tmpEndA);
        //Integer[] hashNext    = buildHashOfRegion(nextBmp   , tmpStartB + mShadowHeight, tmpEndB);            
        //Log.d(TAG, "region build time " + (System.currentTimeMillis() - regionStartTime));

        int length = tmpEndA - tmpStartA - mShadowHeight-1;
        int matchLength = len;//arrayHeadTailMatch(hashNext, hashCurrent, length, mThreshold);
        if(length==matchLength)
        {
            Log.d(TAG, "======>>>>>arrayHeadTailMatch  same !!<<<<<<=========== ");
            matchLength=matchLength>0?matchLength:0;
        }
        tmpStartB = tmpStartB + matchLength + mShadowHeight+1;
        tmpEndB=nextBmp.getHeight()-(currentBmp.getHeight()-nEndA);
        Log.d(TAG, "Different region between " + i + " and " + (i + 1)+":====matchLength:"+matchLength);
        return new int[]{tmpStartB,tmpEndB};
    }

    public static String szError="";

    public boolean LenCompose(Bitmap bmp1,Bitmap bmp2, int nMoveLen,int nbmp2Paddingbottom) throws IOException
    {
        Ctools.PrintLog("===============LenCompose================"+nMoveLen+"/"+nbmp2Paddingbottom);

        Region[] regions = new Region[2];

        FastBitmapReader fbr1Part = null, fbr2Full = null;
        int fullHeight = 0, fullWidth = 0;
        boolean bNoDrawBottom=false;

        fbr1Part = new FastBitmapReader(bmp1,true);
        fullWidth = fbr1Part.getWidth();
        fbr2Full = new FastBitmapReader(bmp2,true);

        //检测边界
        int[] finds=findStartAndEnd(fbr1Part, fbr2Full);
        int nStartA=finds[0];
        int nEndA=finds[1];

        if(fbr1Part.getHeight()==fbr2Full.getHeight())
        {
            if(nStartA==nEndA)
            {
                Ctools.PrintLog("no need to compose,nStartA==nEndA: "+nStartA);
                return true;
            }
            else if(nStartA==0&&nEndA==fbr1Part.getHeight()-1)
            {
                Ctools.PrintLog("no need to compose,same pic");
                return true;
            }
        }

        regions[0] = new Region(nStartA,nEndA);
        fullHeight += fbr1Part.getHeight();
        Ctools.PrintLog("Region 0: startLine:"+nStartA+" endLine:"+nEndA);

        int lenStartA=nStartA;
        int lenEndA=bmp1.getHeight()-nEndA;
        Ctools.PrintLog("lenStartA:"+lenStartA+" / lenEndA:"+lenEndA+" >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        //填充region[1]
        int tmpStartB=bmp2.getHeight()-lenEndA-nMoveLen;
        int tmpEndB=bmp2.getHeight()-lenEndA;
        Ctools.PrintLog("before---------tmpStartB:"+tmpStartB+"/tmpEndB:"+tmpEndB);
        tmpEndB-=nbmp2Paddingbottom;
        if(tmpEndB<0||tmpStartB<0||tmpEndB<tmpStartB)
        {
            //下选框边界已经超过移动距离，不需要合成
            Ctools.PrintLog("out of range, no need to compose:"+tmpStartB+"/"+tmpEndB);
            return true;
        }
        Ctools.PrintLog("---------nbmp2Paddingbottom:"+nbmp2Paddingbottom);
        if(nbmp2Paddingbottom>0&&nbmp2Paddingbottom-(fbr2Full.getHeight()-nEndA)>0)
        {
            Ctools.PrintLog("move len more than Bottom");
            bNoDrawBottom=true;
            tmpStartB-=fbr2Full.getHeight()-nEndA;
        }

        regions[1] = new Region(tmpStartB,tmpEndB);
        fullHeight += tmpEndB - tmpStartB;

        Ctools.PrintLog("Region 1 : startLine:"+tmpStartB+" endLine:"+tmpEndB);

        //回收图片内存，避免内存泄漏
        fbr1Part.recycle();
        fbr1Part = null;
        fbr2Full.recycle();
        fbr2Full = null;
        //=======================

        Log.d(TAG, "fullHeight = " + fullHeight);

        //开始拼接图片
        FastBitmapWriter writer = new FastBitmapWriter(fullWidth, fullHeight);
        int totalHeight = 0;
        FastBitmapReader bmpReaderWithRecycle;
        //BitmapRegionDecoder decoder;

        ///================================
        Region region = regions[0];
        Ctools.PrintLog("-------File index 1:"+region);
        //decoder = BitmapRegionDecoder.newInstance(bmp1.mBuffer,0,bmp1.getAllocationByteCount(), false);
        //把第一张图片，从0到enline的位置取出，绘制到fullbitmap的0到enline位置
        Ctools.PrintLog("index 0,getpic from 0 to enline:"+region.endLine);
        Ctools.PrintLog("index 0,draw pic from 0 to fullbmp from 0, len:"+region.endLine);
        //Bitmap bm=decoder.decodeRegion(new Rect(0, 0, fullWidth, region.endLine), null);
        Bitmap bm=Bitmap.createBitmap(bmp1, 0, 0, fullWidth, region.endLine);
        bmpReaderWithRecycle = new FastBitmapReader(bm);
        writer.writeBitmapRegion(bmpReaderWithRecycle, 0, 0, region.endLine);
        bmpReaderWithRecycle.recycle();

        int bmpHeight = bmp1.getHeight();
        //Ctools.PrintLog("bmpHeight:"+bmpHeight+"   region.endLine:"+region.endLine+"  fullHeight:"+fullHeight);
        Ctools.PrintLog("index 0,getpic from enline ["+region.endLine+"] to bmpHeight ["+(bmpHeight)+"]");
        Ctools.PrintLog("index 0,draw pic from 0 to fullbmp from  "+(fullHeight - (bmpHeight - region.endLine))+
                        ", len:"+(bmpHeight - region.endLine));

        if(bNoDrawBottom)//如果Paddingbottom大于固底
        {
            //continue;
        }
        //然后把第一张图片的从enline开始之后部分取出，绘制到fullbitmap末尾部分
        if (region.endLine < bmpHeight - 1) {
            // Dirty Fix: Ignore any NPE here.
            try {
                //Bitmap bmpTemp=decoder.decodeRegion(new Rect(0, region.endLine, fullWidth, bmpHeight), null);
                Bitmap bmpTemp=Bitmap.createBitmap(bmp1, 0, region.endLine, fullWidth, bmpHeight-region.endLine);
                Ctools.PrintLog("bmpTemp is null : "+(bmpTemp==null));
                bmpReaderWithRecycle = new FastBitmapReader(bmpTemp);
                writer.writeBitmapRegion(bmpReaderWithRecycle, 0, fullHeight - (bmpHeight - region.endLine), bmpHeight - region.endLine);
                bmpReaderWithRecycle.recycle();
            } catch (NullPointerException npe) {
                npe.printStackTrace();
            }
        }

        totalHeight += region.endLine;

        //---------------------------------------
        region = regions[1];
            //decoder.recycle();
            //decoder = BitmapRegionDecoder.newInstance(bmp2.mBuffer,0,bmp2.getAllocationByteCount(), false);
            //依次绘制在fullbitmap中间追加绘制非重叠部分
            Ctools.PrintLog("index "+1+",getpic from startLine["+region.startLine+"] to endLine["+region.endLine+"]");
            Ctools.PrintLog("index "+1+",draw pic from 0 to fullbmp from  "+totalHeight+", len: "+(region.endLine - region.startLine));
            int nDrawlen=region.endLine - region.startLine;
            if(nDrawlen==0)
            {
                Ctools.PrintLog("nothing need to draw, nDrawlen is 0");
                return true;
            }
            Ctools.PrintLog(bmp2.getHeight()+"/"+region);
            Bitmap bmpTemp2=Bitmap.createBitmap(bmp2, 0, region.startLine, fullWidth, region.endLine-region.startLine);
            Ctools.PrintLog("bmpTemp2 is null : "+(bmpTemp2==null));
            bmpReaderWithRecycle = new FastBitmapReader(bmpTemp2);//decoder.decodeRegion(new Rect(0, region.startLine, fullWidth, region.endLine), null));
            writer.writeBitmapRegion(bmpReaderWithRecycle, 0, totalHeight, region.endLine - region.startLine);
            bmpReaderWithRecycle.recycle();

            totalHeight += (region.endLine - region.startLine);

        //decoder.recycle();
    ///================================

        // 保存合成后的图片
        File fOutFile = new File(Ctools.szImagePath + Ctools.szOutImageName);
        fOutFile.delete();
        fOutFile.createNewFile();

        Bitmap out = writer.getBitmap();
        writer.recycle();

        FileOutputStream ops = new FileOutputStream(fOutFile);
        out.compress(Bitmap.CompressFormat.PNG, 100, ops);
        ops.close();

        out.recycle();

        return true;

    }
    
    public boolean LenCompose1(Bitmap bmp2,int nMoveLen,int nLenPaddingbottom,int nlenPaddingTop,boolean bDoneClick)
    {
    	long time0 = System.currentTimeMillis();
    	
    	Ctools.PrintLog("===============LenCompose1================");
        //填充regions，图片和region是一一对应的
        Region[] regions = new Region[2];

        FastBitmapReader currentBmp = null, nextBmp = null;
        int fullHeight = 0, fullWidth = 0;

        currentBmp = new FastBitmapReader(mMergedBitmap, true);
        fullWidth = currentBmp.getWidth();
        nextBmp = new FastBitmapReader(bmp2,true);

        //检测边界
        int[] finds = findStartAndEnd(currentBmp, nextBmp);

        int nStartA = finds[0];
        int nEndA = finds[1];
        Ctools.PrintLog("nStartA:"+nStartA+"      nEndA:"+nEndA);
        if(!bDoneClick && currentBmp.getHeight()==nextBmp.getHeight()) {
            if(nStartA == nEndA) {
                Ctools.PrintLog("no need to compose,nStartA==nEndA: "+nStartA);
                return true;
            } else if(nStartA == 0 && nEndA == currentBmp.getHeight() - 1) {
                Ctools.PrintLog("no need to compose,same pic");
                return true;
            }
        }

        //计算bmp2的截取区域
        int lenEndA = currentBmp.getHeight()-nEndA;
        int tmpStartB = nextBmp.getHeight() - lenEndA - nMoveLen;
        int tmpEndB = nextBmp.getHeight() - lenEndA;
        
        android.util.Log.i("==MyTest==", "tmpStartB = " + tmpStartB + ",tmpEndB");
        
        
        //保存bmp1,bmp2图的regions
        regions[0] = new Region(nStartA,nEndA);
        regions[1] = new Region(tmpStartB,tmpEndB);
        fullHeight = nEndA + nextBmp.getHeight() - tmpStartB;

        //回收图片内存，避免内存泄漏
        currentBmp.recycle();
        currentBmp = null;
        nextBmp.recycle();
        currentBmp = null;

        Ctools.PrintLog( "-----------fullHeight = " + fullHeight);

        Bitmap oldBitmap;
        long time1;
    	if (tmpStartB > 0) {
			//开始拼接图片
			FastBitmapWriter writer = new FastBitmapWriter(fullWidth,
					fullHeight);
			int totalHeight = 0;
			Bitmap bm1 = null;
			FastBitmapReader fbmp1 = null;
			Bitmap bm2 = null;
			FastBitmapReader fbmp2 = null;
			//第一张图片bmp1的写入
			try {
				bm1 = Bitmap.createBitmap(mMergedBitmap, 0, 0, fullWidth,
						regions[0].endLine);
				fbmp1 = new FastBitmapReader(bm1);
				writer.writeBitmapRegion(fbmp1, 0, totalHeight,
						regions[0].endLine);
				totalHeight += regions[0].endLine;
				Ctools.PrintLog("------------totalHeight1 = " + totalHeight);
			} catch (OutOfMemoryError error) {
				writer.recycle();
				writer = null;
				return false;
			} finally {
				if (bm1 != null) {
					bm1.recycle();
					bm1 = null;
				}
				if (fbmp1 != null) {
					fbmp1.recycle();
					fbmp1 = null;
				}
			}
			//第二张图片bmp2的写入
			try {
				bm2 = Bitmap.createBitmap(bmp2, 0, regions[1].startLine,
						fullWidth, bmp2.getHeight() - regions[1].startLine);
				fbmp2 = new FastBitmapReader(bm2);
				writer.writeBitmapRegion(fbmp2, 0, totalHeight,
						bmp2.getHeight() - regions[1].startLine);
				totalHeight += bmp2.getHeight() - regions[1].startLine;
				Ctools.PrintLog("------------totalHeight2 = " + totalHeight);
			} catch (OutOfMemoryError error) {
				writer.recycle();
				writer = null;
				return false;
			} finally {
				if (bm2 != null) {
					bm2.recycle();
					bm2 = null;
				}
				if (fbmp2 != null) {
					fbmp2.recycle();
					fbmp2 = null;
				}
				if (bmp2 != null) {
					bmp2.recycle();
					bmp2 = null;
				}
			}
			oldBitmap = mMergedBitmap;
			mMergedBitmap = writer.getBitmap();
			oldBitmap.recycle();
			writer.recycle();
			writer = null;
		}
    	
    	time1 = System.currentTimeMillis();
		android.util.Log.i("==MyTest==", "time1: " + (time1 - time0));
    	
		if (bDoneClick){
        	oldBitmap = mMergedBitmap;
        	mMergedBitmap = Bitmap.createBitmap(mMergedBitmap,0,nlenPaddingTop, mMergedBitmap.getWidth(), mMergedBitmap.getHeight() - nLenPaddingbottom - nlenPaddingTop);
        	oldBitmap.recycle();

            Ctools.PrintLog( "-----------fullHeight = " + mMergedBitmap.getHeight());
            Ctools.PrintLog( "-----------nlenPaddingTop = " + nlenPaddingTop);
            Ctools.PrintLog( "-----------nLenPaddingbottom = " + nLenPaddingbottom);

            long time2 = System.currentTimeMillis();
        	android.util.Log.i("==MyTest==", "time2: " + (time2 - time1));
        	
            // save image
            FileOutputStream ops = null;
            try {
                ops = new FileOutputStream(Ctools.szImagePath + Ctools.szOutImageName);
                mMergedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, ops);
                ops.flush();
                ops.close();
            } catch (Exception e) {
            	android.util.Log.e("==MyTest==", "TCTcompose2()# save image failed", e);
                szError="FileOutputStream:"+e;
            }
            
            long time3 = System.currentTimeMillis();
        	android.util.Log.i("==MyTest==", "time3: " + (time3 - time1));
        	
            mMergedBitmap.recycle();
            mMergedBitmap = null;
            
            long time4 = System.currentTimeMillis();
        	android.util.Log.i("==MyTest==", "time4: " + (time4 - time3));
        }
        
        return true;
    }
    
    public void setMergedBitmap(Bitmap bitmap) {
    	mMergedBitmap = bitmap;
    }
    
    public Bitmap getMergedBitmap() {
    	return mMergedBitmap;
    }


    //[FR 857010]TCT.NB junqiang.shi 2015.11.23 ADD Begin
    /**
     * 由TCTcompose改写而来；每截屏一次，合成一次图片；使用逻辑拼接的方法
     * @param bmp1
     * @param bmp2
     * @param listener
     * @param nLenPaddingbottom
     * @param nlenPaddingTop
     * @return
     */
    public boolean TCTcompose1(Bitmap bmp1,Bitmap bmp2, ProgressListener listener,int nLenPaddingbottom,int nlenPaddingTop)
    {
        Ctools.PrintLog("===============TCTcompose1================");

        //循环填充regions，图片和region是一一对应的
        Region[] regions = new Region[2];

        FastBitmapReader currentBmp = null, nextBmp = null;
        int fullHeight = 0, fullWidth = 0;

        currentBmp = new FastBitmapReader(bmp1,true);
        fullWidth = currentBmp.getWidth();
        nextBmp = new FastBitmapReader(bmp2,true);

        //检测边界
        int[] finds=findStartAndEnd(currentBmp, nextBmp);
        int nStartA=finds[0];
        int nEndA=finds[1];

        if(currentBmp.getHeight()==nextBmp.getHeight())
        {
            if(nStartA==nEndA)
            {
                Ctools.PrintLog("no need to compose,nStartA==nEndA: "+nStartA);
                return true;
            }
            else if(nStartA==0&&nEndA==currentBmp.getHeight()-1)
            {
                Ctools.PrintLog("no need to compose,same pic");
                return true;
            }
        }

        // 查找[中间部分]公共区域
        int[] commons=FindCommonSpaceOptimize(currentBmp, nextBmp, nStartA, nEndA, nLenPaddingbottom, nlenPaddingTop);
        int tmpStartA=commons[0];
        int tmpEndA=commons[1];
        int tmpStartB=commons[2];
        int tmpEndB=commons[3];

        //保存bmp1,bmp2图的regions
        regions[0] = new Region(tmpStartA,tmpEndA);
        regions[1] = new Region(tmpStartB,tmpEndB);
        fullHeight = tmpEndA + (tmpEndB - tmpStartB) + (nextBmp.getHeight() - tmpEndB);

        //回收图片内存，避免内存泄漏
        currentBmp.recycle();
        currentBmp = null;
        nextBmp.recycle();
        currentBmp = null;

        Ctools.PrintLog( "-----------fullHeight = " + fullHeight);

        //开始拼接图片
        FastBitmapWriter writer = new FastBitmapWriter(fullWidth, fullHeight);
        int totalHeight = 0;
        Bitmap bm1;
        FastBitmapReader fbmp1;
        Bitmap bm2;
        FastBitmapReader fbmp2;
        Bitmap bmTail;
        FastBitmapReader fbmpTail;

        //第一张图片bmp1的写入
        bm1=Bitmap.createBitmap(bmp1, 0, 0, fullWidth, regions[0].endLine);
        fbmp1 = new FastBitmapReader(bm1);
        writer.writeBitmapRegion(fbmp1, 0, totalHeight, regions[0].endLine);
        fbmp1.recycle();
        totalHeight += regions[0].endLine;
        Ctools.PrintLog( "------------totalHeight1 = " + totalHeight);

        //第二张图片bmp2的写入
        bm2 = Bitmap.createBitmap(bmp2, 0, regions[1].startLine, fullWidth, regions[1].endLine - regions[1].startLine);
        fbmp2 = new FastBitmapReader(bm2);
        writer.writeBitmapRegion(fbmp2, 0, totalHeight, regions[1].endLine - regions[1].startLine);
        fbmp2.recycle();
        totalHeight += regions[1].endLine - regions[1].startLine;
        Ctools.PrintLog( "------------totalHeight2 = " + totalHeight);

        //bmp2结尾的写入
        bmTail = Bitmap.createBitmap(bmp2, 0, regions[1].endLine, fullWidth, bmp2.getHeight() - regions[1].endLine);
        fbmpTail = new FastBitmapReader(bmTail);
        writer.writeBitmapRegion(fbmpTail, 0, totalHeight, bmp2.getHeight() - regions[1].endLine);
        fbmpTail.recycle();
        totalHeight += bmp2.getHeight() - regions[1].endLine;
        Ctools.PrintLog( "------------totalHeight3 = " + totalHeight);

        // 保存合成后的图片
        File fOutFile = new File(Ctools.szImagePath + Ctools.szOutImageName);
        fOutFile.delete();
        try
        {
            fOutFile.createNewFile();
        }
        catch (IOException e)
        {
            szError="createNewFile:"+e;
        }

        Bitmap out = writer.getBitmap();
        writer.recycle();

        FileOutputStream ops = null;
        try {
            ops = new FileOutputStream(fOutFile);
            out.compress(Bitmap.CompressFormat.PNG, 100, ops);
            ops.close();
        }
        catch (Exception e)
        {
            szError="FileOutputStream:"+e;
        }

        out.recycle();

        return true;
    }
    //[FR 857010]TCT.NB junqiang.shi 2015.11.23 ADD End

    //[FR 857010]TCT.NB junqiang.shi 2015.11.23 ADD Begin
    /**
     * 由TCTcompose改写而来；每截屏一次，合成一次图片；用最后去头尾的方法合成图片
     * @param bmp1
     * @param bmp2
     * @param nLenPaddingbottom
     * @param nlenPaddingTop
     * @param bDoneClick
     * @return
     */
    public boolean TCTcompose2(/*Bitmap bmp1, */Bitmap bmp2,int nLenPaddingbottom,int nlenPaddingTop,boolean bDoneClick)
    {
    	android.util.Log.i("==MyTest==", "TCTcompose2()");
    	long time0 = System.currentTimeMillis();
    	Ctools.PrintLog("===============TCTcompose2================");

        Bitmap oldBitmap;
        long time1;
	    //循环填充regions，图片和region是一一对应的
		Region[] regions = new Region[2];
		FastBitmapReader currentBmp = null, nextBmp = null;
		int fullHeight = 0, fullWidth = 0;
		currentBmp = new FastBitmapReader(mMergedBitmap, true);
		fullWidth = currentBmp.getWidth();
		nextBmp = new FastBitmapReader(bmp2, true);
		//检测边界
		int[] finds = findStartAndEnd(currentBmp, nextBmp);
		int nStartA = finds[0];
		int nEndA = finds[1];
		Ctools.PrintLog("nStartA:" + nStartA + "      nEndA:" + nEndA);
		android.util.Log.i("==MyTest==", "TCTcompose2()# 1");
		if (!bDoneClick && currentBmp.getHeight() == nextBmp.getHeight()) {
			android.util.Log.i("==MyTest==", "TCTcompose2()# 2");
			if (nStartA == nEndA) {
				android.util.Log.i("==MyTest==", "TCTcompose2()# 3");
				Ctools.PrintLog("no need to compose,nStartA==nEndA: "
						+ nStartA);
				return true;
			} else if (nStartA == 0 && nEndA == currentBmp.getHeight() - 1) {
				android.util.Log.i("==MyTest==", "TCTcompose2()# 4");
				Ctools.PrintLog("no need to compose,same pic");
				return true;
			}
		}
			// 查找[中间部分]公共区域
		Ctools.PrintLog("子线程2*****************************");
		android.util.Log.i("==MyTest==", "FindCommonSpace1# nStartA: "
				+ nStartA + ", nEndA: " + nEndA);
		int[] commons = FindCommonSpace1(currentBmp, nextBmp, nStartA, nEndA);
		int tmpStartB = commons[0];
		int tmpEndB = commons[1];
		//保存bmp1,bmp2图的regions
		regions[0] = new Region(nStartA, nEndA);
		regions[1] = new Region(tmpStartB, tmpEndB);
		fullHeight = nEndA + nextBmp.getHeight() - tmpStartB;
		//回收图片内存，避免内存泄漏
		currentBmp.recycle();
		currentBmp = null;
		nextBmp.recycle();
		currentBmp = null;
		Ctools.PrintLog("-----------fullHeight = " + fullHeight);
		
		if (tmpStartB < tmpEndB) {	
			//开始拼接图片
			FastBitmapWriter writer = new FastBitmapWriter(fullWidth,
					fullHeight);
			int totalHeight = 0;
			Bitmap bm1 = null;
			FastBitmapReader fbmp1 = null;
			Bitmap bm2 = null;
			FastBitmapReader fbmp2 = null;
			//第一张图片bmp1的写入
			try {
				bm1 = Bitmap.createBitmap(mMergedBitmap, 0, 0, fullWidth,
						regions[0].endLine);
				fbmp1 = new FastBitmapReader(bm1);
				writer.writeBitmapRegion(fbmp1, 0, totalHeight,
						regions[0].endLine);
				totalHeight += regions[0].endLine;
				Ctools.PrintLog("------------totalHeight1 = " + totalHeight);
			} catch (OutOfMemoryError error) {
				writer.recycle();
				writer = null;
				return false;
			} finally {
				if (bm1 != null) {
					bm1.recycle();
					bm1 = null;
				}
				if (fbmp1 != null) {
					fbmp1.recycle();
					fbmp1 = null;
				}
			}
			//第二张图片bmp2的写入
			try {
				bm2 = Bitmap.createBitmap(bmp2, 0, regions[1].startLine,
						fullWidth, bmp2.getHeight() - regions[1].startLine);
				fbmp2 = new FastBitmapReader(bm2);
				writer.writeBitmapRegion(fbmp2, 0, totalHeight,
						bmp2.getHeight() - regions[1].startLine);
				totalHeight += bmp2.getHeight() - regions[1].startLine;
				Ctools.PrintLog("------------totalHeight2 = " + totalHeight);
			} catch (OutOfMemoryError error) {
				writer.recycle();
				writer = null;
				return false;
			} finally {
				if (bm2 != null) {
					bm2.recycle();
					bm2 = null;
				}
				if (fbmp2 != null) {
					fbmp2.recycle();
					fbmp2 = null;
				}
				if (bmp2 != null) {
					bmp2.recycle();
					bmp2 = null;
				}
			}
			oldBitmap = mMergedBitmap;
			mMergedBitmap = writer.getBitmap();
			oldBitmap.recycle();
			writer.recycle();
			writer = null;
		}

    	time1 = System.currentTimeMillis();
		android.util.Log.i("==MyTest==", "time1: " + (time1 - time0));

		if (bDoneClick){
        	oldBitmap = mMergedBitmap;
        	mMergedBitmap = Bitmap.createBitmap(mMergedBitmap,0,nlenPaddingTop, mMergedBitmap.getWidth(), mMergedBitmap.getHeight() - nLenPaddingbottom - nlenPaddingTop);
        	oldBitmap.recycle();
            Ctools.PrintLog( "-----------fullHeight = " + mMergedBitmap.getHeight());
            Ctools.PrintLog( "-----------nlenPaddingTop = " + nlenPaddingTop);
            Ctools.PrintLog( "-----------nLenPaddingbottom = " + nLenPaddingbottom);

            long time2 = System.currentTimeMillis();
        	android.util.Log.i("==MyTest==", "time2: " + (time2 - time1));

            // save image
            FileOutputStream ops = null;
            try {
                ops = new FileOutputStream(Ctools.szImagePath + Ctools.szOutImageName);
                mMergedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, ops);
                ops.flush();
                ops.close();
                android.util.Log.e("==MyTest==", "TCTcompose2()# image saved");
            } catch (Exception e) {
            	android.util.Log.e("==MyTest==", "TCTcompose2()# save image failed", e);
                szError="FileOutputStream:"+e;
            }
            
            mMergedBitmap.recycle();
            mMergedBitmap = null;
            long time3 = System.currentTimeMillis();
        	android.util.Log.i("==MyTest==", "time3: " + (time3 - time2));
        }
        
        return true;
    }
    //[FR 857010]TCT.NB junqiang.shi 2015.11.23 ADD End

    public boolean TCTcompose(Bitmap bmp1,Bitmap bmp2, ProgressListener listener,int nLenPaddingbottom)
    {
        Ctools.PrintLog("===============TCTcompose================");

        //循环填充regions，图片和region是一一对应的
        Region[] regions = new Region[2];

        FastBitmapReader currentBmp = null, nextBmp = null;
        int fullHeight = 0, fullWidth = 0;

        for (int i = 0; i < 1; i++)
        {
            //（设计模式-观察者模式）通知listener进度
            if (listener != null) {
                listener.onAnalyzingImage(i + 1, i + 2, 2);
            }

            //准备元素
            if (i==0)
            {
                currentBmp = new FastBitmapReader(bmp1,true);
                fullWidth = currentBmp.getWidth();
            }
            nextBmp = new FastBitmapReader(bmp2,true);

            //检测边界
            int[] finds=findStartAndEnd(currentBmp, nextBmp);
            int nStartA=finds[0];
            int nEndA=finds[1];

            if(currentBmp.getHeight()==nextBmp.getHeight())
            {
                if(nStartA==nEndA)
                {
                    Ctools.PrintLog("no need to compose,nStartA==nEndA: "+nStartA);
                    return true;
                }
                else if(nStartA==0&&nEndA==currentBmp.getHeight()-1)
                {
                    Ctools.PrintLog("no need to compose,same pic");
                    return true;
                }
            }
            //填充region[0]
            if (i == 0) {
                regions[0] = new Region(nStartA,nEndA);
                fullHeight += currentBmp.getHeight();
                Ctools.PrintLog("Region 0: startLine:"+nStartA+" endLine:"+nEndA);
            }

            // 查找[中间部分]公共区域
            int[] commons=FindCommonSpace(i, currentBmp, nextBmp, nStartA, nEndA);
            int tmpStartB=commons[0];
            int tmpEndB=commons[1];

            tmpEndB-=nLenPaddingbottom;
            if(tmpEndB<0)
            {
                //下选框边界已经超过移动距离，不需要合成
                return true;
            }

            //保存下一张图的region
            regions[i + 1] = new Region(tmpStartB,tmpEndB);
            fullHeight += tmpEndB - tmpStartB;

            Ctools.PrintLog("Region "+(i+1)+": startLine:"+tmpStartB+" endLine:"+tmpEndB);

            //回收图片内存，避免内存泄漏
            currentBmp.recycle();
            currentBmp = nextBmp;
            nextBmp = null;
        }

        //回收图片内存，避免内存泄漏
        currentBmp.recycle();
        currentBmp = null;

        Ctools.PrintLog( "fullHeight = " + fullHeight);

        if (listener != null)
        {
            listener.onComposingImage();
        }

        //开始拼接图片
        FastBitmapWriter writer = new FastBitmapWriter(fullWidth, fullHeight);
        int totalHeight = 0;
        FastBitmapReader bmp;
        // BitmapRegionDecoder decoder;
        for (int i = 0; i < 2; i++) {
//            try {
//                decoder = BitmapRegionDecoder.newInstance(FImages[i].getAbsolutePath(), false);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }

            Region region = regions[i];
           // Ctools.PrintLog("-------File index:["+i+"]:"+FImages[i].getName()+region);
            if (i == 0) {
                //把第一张图片，从0到enline的位置取出，绘制到fullbitmap的0到enline位置
                Ctools.PrintLog("index 0,getpic from 0 to enline:"+region.endLine);
                Ctools.PrintLog("index 0,draw pic from 0 to fullbmp from 0, len:"+region.endLine);
                //Bitmap bm=decoder.decodeRegion(new Rect(0, 0, fullWidth, region.endLine), null);
                Bitmap bm=Bitmap.createBitmap(bmp1, 0, 0, fullWidth, region.endLine);
                bmp = new FastBitmapReader(bm);
                writer.writeBitmapRegion(bmp, 0, 0, region.endLine);
                bmp.recycle();

                int bmpHeight = bmp1.getHeight();
                //Ctools.PrintLog("bmpHeight:"+bmpHeight+"   region.endLine:"+region.endLine+"  fullHeight:"+fullHeight);
                Ctools.PrintLog("index 0,getpic from enline ["+region.endLine+"] to bmpHeight ["+(bmpHeight)+"]");
                Ctools.PrintLog("index 0,draw pic from 0 to fullbmp from  "+(fullHeight - (bmpHeight - region.endLine))+
                                ", len:"+(bmpHeight - region.endLine));
                //然后把第一张图片的从enline开始之后部分取出，绘制到fullbitmap末尾部分
                if (region.endLine < bmpHeight - 1) {
                    // Dirty Fix: Ignore any NPE here.
                    try {
                        //Bitmap bmpTemp=decoder.decodeRegion(new Rect(0, region.endLine, fullWidth, bmpHeight), null);
                        Bitmap bmpTemp=Bitmap.createBitmap(bmp1, 0, region.endLine, fullWidth, bmpHeight-region.endLine);
                        Ctools.PrintLog("bmpTemp is null : "+(bmpTemp==null));
                        bmp = new FastBitmapReader(bmpTemp);
                        writer.writeBitmapRegion(bmp, 0, fullHeight - (bmpHeight - region.endLine), bmpHeight - region.endLine);
                        bmp.recycle();
                    } catch (NullPointerException npe) {
                        npe.printStackTrace();
                    }
                }

                //canvas.drawBitmap(bmp, src, dst, null);

                totalHeight += region.endLine;

            } else {
                //依次绘制在fullbitmap中间追加绘制非重叠部分
                Ctools.PrintLog("index "+i+",getpic from startLine["+region.startLine+"] to endLine["+region.endLine+"]");
                Ctools.PrintLog("index "+i+",draw pic from 0 to fullbmp from  "+totalHeight+", len: "+(region.endLine - region.startLine));
                int nDrawlen=region.endLine - region.startLine;
                if(nDrawlen==0)
                {
                    Ctools.PrintLog("nothing need to draw, nDrawlen is 0");
                    continue;
                }
                Bitmap bmpTemp=Bitmap.createBitmap(bmp2, 0, region.startLine, fullWidth, region.endLine-region.startLine);
                bmp = new FastBitmapReader(bmpTemp);
                writer.writeBitmapRegion(bmp, 0, totalHeight, region.endLine - region.startLine);
                bmp.recycle();

                totalHeight += (region.endLine - region.startLine);
            }

            //decoder.recycle();
        }

        // 保存合成后的图片
        File fOutFile = new File(Ctools.szImagePath + Ctools.szOutImageName);
        fOutFile.delete();
        try
        {
            fOutFile.createNewFile();
        }
        catch (IOException e)
        {
            szError="createNewFile:"+e;
        }

        Bitmap out = writer.getBitmap();
        writer.recycle();

        FileOutputStream ops = null;
        try {
            ops = new FileOutputStream(fOutFile);
            out.compress(Bitmap.CompressFormat.PNG, 100, ops);
            ops.close();
        }
        catch (Exception e)
        {
            szError="FileOutputStream:"+e;
        }

        out.recycle();

        return true;
    }

    public String compose(File[] images, ProgressListener listener) {
        Region[] regions = new Region[images.length];

        FastBitmapReader currentBmp = null, nextBmp = null;
        int fullHeight = 0, fullWidth = 0;

        for (int i = 0; i < images.length - 1; i++) {
            long thisStartTime = System.currentTimeMillis();

            if (listener != null) {
                listener.onAnalyzingImage(i + 1, i + 2, images.length);
            }

            long decodeStartTime = System.currentTimeMillis();

            // Intented to use thresholding but failed. Help needed.
            if (currentBmp == null) {
                currentBmp = new FastBitmapReader(BitmapFactory.decodeFile(images[i].getAbsolutePath()));
                fullWidth = currentBmp.getWidth();
            }

            nextBmp = new FastBitmapReader(BitmapFactory.decodeFile(images[i + 1].getAbsolutePath()));

            if (DEBUG) {
                Log.d(TAG, "decode time " + (System.currentTimeMillis() - decodeStartTime));
            }

            if (currentBmp.getHeight() != nextBmp.getHeight()) {

                if (DEBUG) {
                    Log.d(TAG, "Height different");
                }

                return null;
            }

            // First, find the max different region of the two bitmaps

            long headStartTime = System.currentTimeMillis();

            // Go through from the first line
            int start = -1;
            for (int j = mStatusBarHeight + 1; j < currentBmp.getHeight(); j++) {
                if (compareLines(currentBmp, nextBmp, j)) {
                    start = j;
                    break;
                }
            }

            if (start == -1) {

                if (DEBUG) {
                    Log.d(TAG, "start line not found");
                }

                return null;
            }

            // Go through from the last line
            int end = -1;
            for (int j = currentBmp.getHeight() - 1; j > start; j--) {
                if (compareLines(currentBmp, nextBmp, j)) {
                    end = j;
                    break;
                }
            }

            if (end == -1) {

                if (DEBUG) {
                    Log.d(TAG, "End line not found");
                }

                return null;
            }

            if (DEBUG) {
                Log.d(TAG, "head match time " + (System.currentTimeMillis() - headStartTime));
            }

            if (i == 0) {
                Region region = new Region();
                region.startLine = start;
                region.endLine = end;
                regions[0] = region;
                fullHeight += currentBmp.getHeight();
            }

            // Second, find out the max common region

            // Generate a hash string of the bitmaps
            long regionStartTime = System.currentTimeMillis();
            Integer[] hashCurrent = buildHashOfRegion(currentBmp, start + mShadowHeight, end);
            Integer[] hashNext = buildHashOfRegion(nextBmp, start + mShadowHeight, end);

            if (DEBUG) {
                Log.d(TAG, "region build time " + (System.currentTimeMillis() - regionStartTime));
            }

            /*if (DEBUG) {
                Log.d(TAG, "hashCurrent = " + hashCurrent);
                Log.d(TAG, "hashNext = " + hashNext);
            }*/

            int length = end - start - mShadowHeight - 1;

            /*String matchSub = null;*/
            List<Long> matchSub = null;
            int matchLength = arrayHeadTailMatch(hashNext, hashCurrent, length, mThreshold);

            /*for (int j = length; j > 0; j--) {
                //List<Long> hashSubNext = buildHashOfSubregion(hashNext, j);
                //List<Long> hashSubCur = buildHashOfSubregionFromBottom(hashCurrent, j);

                //int result = arrayContainsEx(hashCurrent, hashSub, mThreshold);

            //    if (arrayCompareEx(hashNext, hashCurrent, 0, hashCurrent.size() - j - 1, j, mThreshold)) {
                    //matchSub = hashSubNext;
            //        matchLength = j;
            //        break;
            //    }

                /*String hashSub = buildHashStringOfSubregion(hashNext, j);

                if (hashCurrent.contains(hashSub)) {
                    matchSub = hashSub;
                    matchLength = j;
                    break;
                }*/
            //}

            /*if (matchSub == null) {
                return null;
            }*/

            //int index = arrayContainsEx(hashNext, matchSub, 0.0f);

            start = start/* + index*/ + matchLength + mShadowHeight + 1;

            if (DEBUG) {
                Log.d(TAG, "Different region between " + i + " and " + (i + 1));
                Log.d(TAG, start + "-" + end);
            }

            Region region = new Region();
            region.startLine = start;
            region.endLine = end;
            regions[i + 1] = region;
            fullHeight += end - start;

            if (DEBUG) {
                Log.d(TAG, i + " total time " + (System.currentTimeMillis() - thisStartTime));
            }

            currentBmp.recycle();
            currentBmp = nextBmp;
            nextBmp = null;
        }


        currentBmp.recycle();
        currentBmp = null;

        /*currentBmp = BitmapFactory.decodeFile(images[0].getAbsolutePath());
        nextBmp = BitmapFactory.decodeFile(images[1].getAbsolutePath());

        if (DEBUG) {
            Log.d(TAG, "hash1: " + getHashOfLine(currentBmp, 150));
            Log.d(TAG, "hash2: " + getHashOfLine(nextBmp, 150));
        }*/

        if (DEBUG) {
            Log.d(TAG, "fullHeight = " + fullHeight);
        }

        if (listener != null) {
            listener.onComposingImage();
        }

        //Bitmap out = Bitmap.createBitmap(fullWidth, fullHeight, Bitmap.Config.ARGB_8888);
        //Canvas canvas = new Canvas(out);
        FastBitmapWriter writer = new FastBitmapWriter(fullWidth, fullHeight);
        int totalHeight = 0;
        FastBitmapReader bmp;
        BitmapRegionDecoder decoder;
        for (int i = 0; i < images.length; i++) {
            //bmp = new FastBitmapReader(BitmapFactory.decodeFile(images[i].getAbsolutePath()));
            try {
                decoder = BitmapRegionDecoder.newInstance(images[i].getAbsolutePath(), false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Region region = regions[i];
            //int height = currentBmp.getHeight();

            //Rect rect = new Rect(0, 0, fullWidth, fullHeight);

            /*Rect src = new Rect();
            Rect dst = new Rect();
            src.left = 0;
            src.right = fullWidth;
            dst.left = 0;
            dst.right = fullWidth;*/
            if (i == 0) {
                /*src.top = 0;
                src.bottom = region.endLine;
                dst.top = 0;
                dst.bottom = src.bottom;*/

                bmp = new FastBitmapReader(decoder.decodeRegion(new Rect(0, 0, fullWidth, region.endLine), null));
                writer.writeBitmapRegion(bmp, 0, 0, region.endLine);
                bmp.recycle();

                //canvas.drawBitmap(bmp, src, dst, null);

                /*src.top = region.endLine;
                src.bottom = bmp.getHeight();
                dst.bottom = fullHeight;
                dst.top = dst.bottom - (src.bottom - src.top);*/

                int bmpHeight = decoder.getHeight();

                if (region.endLine < bmpHeight - 1) {
                    // Dirty Fix: Ignore any NPE here.
                    try {
                        bmp = new FastBitmapReader(decoder.decodeRegion(new Rect(0, region.endLine, fullWidth, bmpHeight), null));
                        writer.writeBitmapRegion(bmp, 0, fullHeight - (bmpHeight - region.endLine), bmpHeight - region.endLine);
                        bmp.recycle();
                    } catch (NullPointerException npe) {

                    }
                }

                //canvas.drawBitmap(bmp, src, dst, null);

                totalHeight += region.endLine;

            } else {
                /*src.top = region.startLine;
                src.bottom = region.endLine;
                dst.top = totalHeight;
                dst.bottom = dst.top + (src.bottom - src.top);*/

                //canvas.drawBitmap(bmp, src, dst, null);

                bmp = new FastBitmapReader(decoder.decodeRegion(new Rect(0, region.startLine, fullWidth, region.endLine), null));
                writer.writeBitmapRegion(bmp, 0, totalHeight, region.endLine - region.startLine);
                bmp.recycle();

                totalHeight += (region.endLine - region.startLine);
            }

            decoder.recycle();
        }

        // Write
        File outFile = new File(Ctools.szImagePath + Ctools.szOutImageName);
        if (outFile.exists()) {
            outFile.delete();
        }

        try {
            outFile.createNewFile();
        } catch (IOException e) {

        }

        Bitmap out = writer.getBitmap();
        writer.recycle();

        FileOutputStream opt = null;
        try {
            opt = new FileOutputStream(outFile);
        } catch (IOException e) {

        }

        if (opt != null) {
            out.compress(Bitmap.CompressFormat.PNG, 100, opt);
        }

        try {
            opt.close();
        } catch (IOException e) {

        }

        out.recycle();

        return outFile.getAbsolutePath();
    }

    /**
     * 比较图片1和图片2，某行是否一致
     * @param bmp1
     * @param bmp2
     * @param line
     * @return   true 有差异；false 没有差异。
     */
    public static  boolean compareLines(FastBitmapReader bmp1, FastBitmapReader bmp2, int line) {
        int diff = 0;
        for (int i = 0; i < bmp1.getWidth(); i++) {
            if (bmp1.getPixel(i, line) != bmp2.getPixel(i, line)) {
                diff++;
            }
        }
        return diff > bmp1.getWidth() / 10 * mThreshold;
    }

    /**
     * 从下面开始比较图片1和2
     * @param bmp1
     * @param bmp2
     * @param linea
     * @param lineb
     * @return
     */
    private boolean compareLinesFromBottom(FastBitmapReader bmp1, FastBitmapReader bmp2, int linea,int lineb) {
        int diff = 0;
        for (int i = 0; i < bmp1.getWidth(); i++) {
            if (bmp1.getPixel(i, linea) != bmp2.getPixel(i, lineb)) {
                diff++;
            }
        }
        return diff > bmp1.getWidth() / 10 * mThreshold;
    }

    /**
     * 返回制定行的像素的 hash code
     * @param bmp
     * @param line
     * @return
     */
    private int getHashOfLine(FastBitmapReader bmp, int line) {
        int start = line * bmp.getWidth();
        int end = start + bmp.getWidth()-50;//cheng mod,排除滚动条的影响
        int[] pixels = Arrays.copyOfRange(bmp.getPixels(), start, end);

        return Arrays.hashCode(pixels);
    }

    /**
     * 给图片的制定区域创建hash数组
     * @param bmp
     * @param start
     * @param end
     * @return
     */
    private Integer[] buildHashOfRegion(FastBitmapReader bmp, final int start, final int end) {
        //List<Long> list = new ArrayList<Long>();
    	android.util.Log.i("==MyTest==", "buildHashOfRegion()# start: " + start + ", end: " + end);
        Integer[] array = new Integer[end - start];

        /*for (int i = start; i < end; i++) {
            array[i - start] = getHashOfLine(bmp, i);
        }*/

        return new MultiThreadTask<FastBitmapReader, Integer>(bmp, array) {
            @Override
            protected void doExecute(FastBitmapReader bmp, int taskStart, int taskLength) {
                for (int i = 0; i < taskLength; i++) {
                    int hash = getHashOfLine(bmp, start + taskStart + i);

                    setResult(taskStart + i, hash);
                }
            }
        }.execute(Runtime.getRuntime().availableProcessors()); // CPU cores as threads

        //return array;
    }

}
