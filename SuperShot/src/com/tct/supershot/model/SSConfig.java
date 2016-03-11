package com.tct.supershot.model;

import java.io.File;

import com.tct.supershot.Ctools;

public class SSConfig
{

    private  int nScreenWidth=-1;//屏幕宽度
    private  int nScreenHeight=-1;//屏幕高度
    private  int nScreenRotation=-1;
    private  int nStatusbarHeight=-1;

    private  int nMovelen=1000;//移动距离
    private  int nBottomPadding=200;//触屏落点起始位置距离底部的距离
    private  int nMoveOffSet=24;//移动偏移量，触屏事件触发，并不完美移动指定距离，这里做修正，修改nMovelen，对应要调整nMoveOffSet
    private  int nMoveDuration=300;

    private static SSConfig ssc=null;

    public SSConfig(int nScreenWidth, int nScreenHeight, int nScreenRotation,
            int nStatusbarHeight, int nMovelen, int nBottomPadding,int nMoveOffSet,int nMoveDuration) {
        this.nScreenWidth = nScreenWidth;
        this.nScreenHeight = nScreenHeight;
        this.nScreenRotation = nScreenRotation;
        this.nStatusbarHeight = nStatusbarHeight;
        this.nMovelen = nMovelen;
        this.nBottomPadding = nBottomPadding;
        this.nMoveOffSet = nMoveOffSet;
        this.nMoveDuration=nMoveDuration;
    }


    public static void init(int nScreenWidth, int nScreenHeight, int nScreenRotation, int nStatusbarHeight,
            int nMovelen,int nBottomPadding,int nMoveOffSet,int nMoveDuration)
    {
//        if(ssc!=null)
//        {
//            throw new IllegalStateException("SSConfig is not null !");
//        }
        ssc=new SSConfig(nScreenWidth, nScreenHeight, nScreenRotation, nStatusbarHeight, nMovelen, nBottomPadding,nMoveOffSet,nMoveDuration);
        Ctools.ClearFolder(new File(Ctools.szImagePath));

        File path=new File(Ctools.szImagePath);
        path.mkdir();
    }

    public static SSConfig getInstance()
    {
        if(ssc==null)
        {
            throw new IllegalStateException("SSConfig is null !");
        }
        return ssc;
    }

    public int getnMoveDuration()
    {
        return nMoveDuration;
    }

    public int getnScreenWidth()
    {
        return nScreenWidth;
    }

    public int getnScreenHeight()
    {
        return nScreenHeight;
    }

    public int getnScreenRotation()
    {
        return nScreenRotation;
    }

    public int getnStatusbarHeight()
    {
        return nStatusbarHeight;
    }

    public int getnMovelen()
    {
        return nMovelen;
    }

    public int getnBottomPadding()
    {
        return nBottomPadding;
    }

    public int getnMoveOffSet()
    {
        return nMoveOffSet;
    }

}
