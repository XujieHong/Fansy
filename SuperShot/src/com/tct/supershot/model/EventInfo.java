package com.tct.supershot.model;

public class EventInfo
{
    public long lDownTime=-1;
    public float fDownx=0;
    public float fDowny=0;
    public int nEventType=-1;

    public EventInfo(long lDownTime, int nEventType,float fDownx, float fDowny)
    {
        this.lDownTime = lDownTime;
        this.fDownx = fDownx;
        this.fDowny = fDowny;
        this.nEventType=nEventType;
    }
}
