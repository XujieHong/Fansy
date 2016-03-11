package info.papdt.pano.support;

import android.graphics.Bitmap;

/*
 * This class is a faster reader of Bitmap
 * Because the method Bitmap.getPixel() is extremely slow.
 * This is an alternative.
 *
 */
public class FastBitmapReader
{
    private int[] mPixels;
    private int mWidth, mHeight;

    public FastBitmapReader(Bitmap bmp) {
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        mPixels = new int[width * height];
        bmp.getPixels(mPixels, 0, width, 0, 0, width, height);
        bmp.recycle(); // Destroy the original bitmap for memory
        mWidth = width;
        mHeight = height;
    }

    public FastBitmapReader(Bitmap bmp,boolean bNoRecyle) {
        if(!bNoRecyle)
        {
            throw new IllegalArgumentException("NoRecyle must be true");
        }
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        mPixels = new int[width * height];
        bmp.getPixels(mPixels, 0, width, 0, 0, width, height);
        //bmp.recycle(); // Destroy the original bitmap for memory
        mWidth = width;
        mHeight = height;
    }

    public FastBitmapReader(int[] pixs, int width, int height) {
    	this.mPixels = pixs;
    	this.mWidth = width;
    	this.mHeight = height;
    }

    /**
     * 获取指定位置的像素值
     * @param x    横坐标
     * @param y    行
     * @return
     */
    public int getPixel(int x, int y) {
        return mPixels[x + y * mWidth];
    }

    public int[] getPixels() {
        return mPixels;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public void recycle() {
        mPixels = null;
    }
}
