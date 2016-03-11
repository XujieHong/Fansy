package com.tct.supershot.model;

import com.tct.supershot.SuperShotActivity;

import android.graphics.Bitmap;

public class InfoForSave
{
    public static final int nType_LenCompose=1;
    public static final int nType_PicCompose=2;

    public int nType=-1;
    public Bitmap bmpCurent;
    public int  nbmp2Paddingbottom=-1;
    public int  nbmp1PaddingTop=-1;
    public SuperShotActivity act;
    public boolean bDoneClick;


    public InfoForSave(int nType, Bitmap bmpCurent,int nbmp2Paddingbottom,int nbmp2PaddingTop,SuperShotActivity act,boolean bDoneClick)
    {
        this.nType = nType;
        this.bmpCurent = bmpCurent;
        this.nbmp2Paddingbottom = nbmp2Paddingbottom;
        this.nbmp1PaddingTop = nbmp2PaddingTop;
        this.act = act;
        this.bDoneClick=bDoneClick;
    }
}
