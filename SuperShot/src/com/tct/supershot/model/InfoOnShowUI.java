package com.tct.supershot.model;

import android.graphics.Bitmap;

import com.tct.supershot.FloatView;
import com.tct.supershot.SuperShotActivity;

public class InfoOnShowUI
{
    public Bitmap bmpScreen;
    public SuperShotActivity act;
    public boolean bDoneClick;

    public InfoOnShowUI(Bitmap bmpScreen,SuperShotActivity act,boolean bDoneClick)
    {
        this.bmpScreen = bmpScreen;
        this.act=act;
        this.bDoneClick=bDoneClick;
    }
}
