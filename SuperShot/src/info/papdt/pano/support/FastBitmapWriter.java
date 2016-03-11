package info.papdt.pano.support;

//import com.tct.supershot.TRect;

import android.graphics.Bitmap;

public class FastBitmapWriter
{
    private int[] mPixels;
    private int mWidth, mHeight;

    public FastBitmapWriter(int width, int height) {
        mPixels = new int[width * height];
        mWidth = width;
        mHeight = height;
    }
    /*
    public FastBitmapWriter(FastBitmapWriter src, TRect srcRect, TRect destRect, boolean recycleSrc) {
    	if (src == null) {
    		if (destRect.width == 0 || destRect.height == 0) {
        		throw new IllegalArgumentException("0 size");
        	}

    		mWidth = destRect.width;
    		mHeight = destRect.height;
    		mPixels = new int[mWidth * mHeight];
    	} else {
    		// generate copy from rect
    		TRect copySrcRect = new TRect(srcRect);
    		copySrcRect.cut(0, 0, src.mWidth, src.mHeight);
    		copySrcRect.set(
    				copySrcRect.x,
    				copySrcRect.y,
    				Math.min(copySrcRect.width, destRect.width),
    				Math.min(copySrcRect.height, destRect.height));
    		
    		// generate copy to rect
    		TRect copyDestRect = new TRect(destRect);
    		copyDestRect.set(
    				copyDestRect.x,
    				copyDestRect.y,
    				copySrcRect.width,
    				copySrcRect.height);
    		
    		// generate real writer rect
    		TRect realDestRect = new TRect(destRect);
    		realDestRect.setLeft(0);
    		realDestRect.setTop(0);

    		//
    		mWidth = realDestRect.width;
    		mHeight = realDestRect.height;
    		mPixels = new int[mWidth * mHeight];
    		
    		for (int i=0; i<copySrcRect.height; i++) {
    			System.arraycopy(
    					src.mPixels,
    					(i+copySrcRect.y)*src.mWidth+copySrcRect.x,
    					mPixels,
    					(i+copyDestRect.y)*realDestRect.width+copyDestRect.x,
    					copySrcRect.width);
    		}

    		if (recycleSrc) {
    			src.recycle();
    		}
    	}
    }*/

    /**
     * 用传入的图片的指定像素填充当前图片的指定像素
     * @param bmp   传入图片
     * @param srcTop  传入图片起始读取位置
     * @param dstTop  目标图片起始写入位置
     * @param length  读写长度
     */
    public void writeBitmapRegion(FastBitmapReader bmp, int srcTop, int dstTop, int length){ 
        if (bmp.getWidth() != mWidth) throw new IllegalArgumentException("width differs");

        int[] srcPixels = bmp.getPixels();
        int srcStart = srcTop * mWidth;
        int dstStart = dstTop * mWidth;
        int totalLength = length * mWidth - 1;

        System.arraycopy(srcPixels, srcStart, mPixels, dstStart, totalLength);
    }

    /**
     * 通过当前存储的像素，返回一张图片
     * @return
     */
    public Bitmap getBitmap() {
        Bitmap bmp = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.RGB_565);

        bmp.setPixels(mPixels, 0, mWidth, 0, 0, mWidth, mHeight);

        return bmp;
    }

    public void recycle() {
        mPixels = null;
    }
/*
    public TRect getRect() {
    	return new TRect(0, 0, mWidth, mHeight);
    }
    */
    
    public int getWidth() {
    	return mWidth;
    }
    
    public int getHeight() {
    	return mHeight;
    }
    /*
    public FastBitmapReader getReader() {
    	return new FastBitmapReader(mPixels, mWidth, mHeight);
    }*/
}
