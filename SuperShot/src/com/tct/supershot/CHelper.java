package com.tct.supershot;

import info.papdt.pano.processor.ScreenshotComposer;

import java.io.File;
import java.io.IOException;

import android.animation.ValueAnimator;
import android.app.Instrumentation;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;

import com.tct.supershot.model.EventInfo;
import com.tct.supershot.model.InfoForSave;
import com.tct.supershot.model.InfoOnShowUI;
import com.tct.supershot.model.SSConfig;

public class CHelper
{
    private static final int nMsg_Down=1;
    private static final int nMsg_move=2;
    private static final int nMsg_Up=3;
    private static final int nMsg_ShowUI=4;

    private static final int nMsg_Save=11;

    public static final String szImagePath=Environment.getExternalStorageDirectory()
            +File. separator+"supershot"+File. separator;
    public static final String szCapImageName="ss.png";
    public static final String szOutImageName="out.png";

    private static Instrumentation is=null;//用于模拟触屏事件

    private HandlerThread moveThead;
    private Handler moveHD;

    private HandlerThread saveThread;
    private Handler saveHD;

    //变量区，初始化必须复位
    private long nlastMoveTime = 0;
    public static boolean bHaveVerticalScrollbar=false;
    public static boolean bHasReachBottom=false;
    public static long reachButtonTime = 0;
    private long animatorStartTime = 0;
    private int nbmp1PaddingTop = 0;//长截屏的起始位置
    private int nbmp2Paddingbottom = 0;//长截屏的截止位置


    public CHelper()
    {
        bHaveVerticalScrollbar=false;
        bHasReachBottom=false;
        reachButtonTime = 0;
        animatorStartTime = 0;

        is=new Instrumentation();

        moveThead=new HandlerThread("1");
        moveThead.start();

        moveHD=new Handler(moveThead.getLooper())
        {
            @Override
            public void handleMessage(Message msg)
            {
            	
                switch (msg.what)
                {
                case nMsg_Down:
                    SendTouchEvent(msg.obj);
                    break;
                case nMsg_move:
                    SendTouchEvent(msg.obj);
                    break;
                case nMsg_Up:
                    SendTouchEvent(msg.obj);
                    break;
                case nMsg_ShowUI:
                	android.util.Log.i("==MyTest==", "nMsg_ShowUI");
                    InfoOnShowUI si=(InfoOnShowUI)msg.obj;
                    FloatView mFloatView=si.act.getFloatView();

                    //为判底保存截屏
                    Bitmap bmpForJudgeBottom=null;
                    //[FR 857010]TCT.NB junqiang.shi 2015.11.12 ADD Begin
                    //判底显示
                    if(!bHaveVerticalScrollbar)
                    {
                        bmpForJudgeBottom=Ctools.GetFloatViewScreenshotBmp(mFloatView.getDefaultRect());
                        boolean bJudgeBottom=Ctools.bIsBottomByCapture(si.bmpScreen, bmpForJudgeBottom);
                        if(bJudgeBottom && !si.bDoneClick)
                        {
                        	bHasReachBottom = true;
                        }
                    }
                    if(bHasReachBottom)
                    {
                        mFloatView.PostHasReachBottom();
                    }
                    //[FR 857010]TCT.NB junqiang.shi 2015.11.12 ADD End

                    //同时线程2保存图片，做图片合成
                    Message msg_Save=saveHD.obtainMessage(nMsg_Save);
                    
                    int nType=bHaveVerticalScrollbar?InfoForSave.nType_LenCompose:InfoForSave.nType_PicCompose;
                    nbmp2Paddingbottom=mFloatView.getDefaultRect().bottom-mFloatView.getSelectRect().bottom;
                    if(nbmp1PaddingTop == 0){
                        nbmp1PaddingTop=mFloatView.getSelectRect().top - mFloatView.getDefaultRect().top;
                    }
                    msg_Save.obj=new InfoForSave(nType, si.bmpScreen, nbmp2Paddingbottom,nbmp1PaddingTop,si.act,si.bDoneClick);
                    saveHD.sendMessage(msg_Save);
                    //界面显示
                    mFloatView.PostTemporaryHide(false);

                    break;
                default:
                    throw new  IllegalArgumentException("wrong message type");
                }
            }
        };

        saveThread=new HandlerThread("save");
        saveThread.start();
        saveHD=new Handler(saveThread.getLooper())
        {
            @Override
            public void handleMessage(Message msg)
            {
                switch (msg.what)
                {
                case nMsg_Save:
                	android.util.Log.i("==MyTest==", "nMsg_Save");
                    InfoForSave si=(InfoForSave) msg.obj;
                    if(si.nType==InfoForSave.nType_LenCompose)
                    {
                        SaveForLen(si.bmpCurent,si.nbmp2Paddingbottom,si.nbmp1PaddingTop,si.act,si.bDoneClick);
                    }
                    else
                    {
                        SaveForPic(si.bmpCurent,si.nbmp2Paddingbottom,si.nbmp1PaddingTop,si.act,si.bDoneClick);
                    }
                    break;
                default:
                    throw new  IllegalArgumentException("wrong message type");
                }
            }
        };
    }

    public void clean()
    {
        moveThead.quitSafely();
        saveThread.quitSafely();
        ScreenshotComposer.getInstance().setMergedBitmap(null);
    }

    public void ScrollDown(final SuperShotActivity act,final FloatView mFloatView,final boolean bDoneClick)
    {
        //隐藏FLoatView
        mFloatView.TemporaryHide(true);

        mFloatView.post(new Runnable()
        {
            @Override
            public void run()
            {
                //截屏
                Bitmap bmpScreen=Ctools.GetFloatViewScreenshotBmp(mFloatView.getDefaultRect());

                //移动和显示
                MoveAndShow(bmpScreen, mFloatView, bDoneClick,act);
            }
        });
    }
    boolean bStart=false;
    private void MoveAndShow(final Bitmap bmpScreen,final FloatView mFloatView,final boolean bDoneClick,final SuperShotActivity act)
    {
        final int nScreenWidth = SSConfig.getInstance().getnScreenWidth();
        final int nScreenHeight = SSConfig.getInstance().getnScreenHeight();
        final int nMovelen = SSConfig.getInstance().getnMovelen();
        final int nBottomPadding = SSConfig.getInstance().getnBottomPadding();
        final int nMoveDuration = SSConfig.getInstance().getnMoveDuration();

        if(!bDoneClick)
        {
            final long lDowntime=SystemClock.uptimeMillis();
            final float fStartX=nScreenWidth/2;
            final float fStartY=nScreenHeight-nBottomPadding;

            ValueAnimator va=ValueAnimator.ofFloat(fStartY,fStartY-nMovelen);
            va.setDuration(nMoveDuration);
            va.setInterpolator(new DecelerateInterpolator());
            va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
            {
                @Override
                public void onAnimationUpdate(ValueAnimator animation)
                {
                    float fValue=(float) ((ValueAnimator) animation).getAnimatedValue();
                    //Ctools.PrintLog("onAnimationUpdate:"+fValue+"");

                    if(fValue==fStartY)//开始
                    {

                        //[FR 857010]TCT.NB junqiang.shi 2015.11.6 ADD Begin
                        if(!bStart)
                        {
                            bStart=true;
                        }
                        else
                        {
                            Message action= moveHD.obtainMessage(nMsg_Down);
                            action.obj=new EventInfo(lDowntime, MotionEvent.ACTION_DOWN, fStartX, fValue);
                            moveHD.sendMessage(action);
                        }
                        //[FR 857010]TCT.NB junqiang.shi 2015.11.6 ADD End

                    }
                    else if(fValue==fStartY-nMovelen)//结束
                    {
                        bStart=false;
                        //避免fly，多发几次微小的move移动
                        for(int i=0;i<20;i++)
                        {
                            Message action= moveHD.obtainMessage(nMsg_move);
                            action.obj=new EventInfo(lDowntime, MotionEvent.ACTION_MOVE, fStartX, fValue);
                            moveHD.sendMessage(action);
                            fValue-=0.01f;
                        }

                        //发送up事件
                        Message action= moveHD.obtainMessage(nMsg_Up);
                        action.obj=new EventInfo(lDowntime, MotionEvent.ACTION_UP, fStartX, fValue);
                        moveHD.sendMessage(action);

                        //恢复UI显示
                        Message actionShowUI= moveHD.obtainMessage(nMsg_ShowUI);
                        actionShowUI.obj=new InfoOnShowUI(bmpScreen,act,bDoneClick);
                        moveHD.sendMessage(actionShowUI);
                        Ctools.PrintLog("####realdonghuashijian:" + (animatorStartTime - System.currentTimeMillis()));
                    }
                    else
                    {
                        Message action= moveHD.obtainMessage(nMsg_move);
                        action.obj=new EventInfo(lDowntime, MotionEvent.ACTION_MOVE, fStartX, fValue);
                        moveHD.sendMessage(action);
                    }
                    Ctools.PrintLog("####realdonghuashijian:" + (animatorStartTime - System.currentTimeMillis()));
                }
                
            });
            va.start();
            animatorStartTime = SystemClock.uptimeMillis();//System.currentTimeMillis();
        }
        else
        {
            Message actionShowUI= moveHD.obtainMessage(nMsg_ShowUI);
            actionShowUI.obj=new InfoOnShowUI(bmpScreen,act,bDoneClick);
            moveHD.sendMessage(actionShowUI);
        }
    }

    private boolean SaveForLen(Bitmap bmpCurrentScreen,int nbmp2Paddingbottom,int nbmp1PaddingTop,SuperShotActivity act,boolean bDoneClick)
    {
    	android.util.Log.i("==MyTest==", "SaveForLen()");
        //如果out文件不存在，创建它，退出
    	if (ScreenshotComposer.getInstance().getMergedBitmap() == null) {
    		ScreenshotComposer.getInstance().setMergedBitmap(bmpCurrentScreen);
    		return true;
    	}


        //读进out文件
/*        Bitmap bmpOut=BitmapFactory.decodeFile(szImagePath + szOutImageName);
        if(bmpOut==null)
        {
            Ctools.PrintLog("waring !!!!!,read bmpOut failed");
            return false;
        }*/

        //合成out文件和bmpCurrentScreen
        int nRealLastMovelen=SSConfig.getInstance().getnMovelen()-SSConfig.getInstance().getnMoveOffSet();
        //if(bHasReachBottom)//这里还是正常的移动Movelen，应该在done的时候才是nLastMoveLen
        if(bDoneClick)
        {
            //[FR 857010]TCT.NB junqiang.shi 2015.11.12 ADD Begin
            if(bHasReachBottom){
            	int nMoveDuration=SSConfig.getInstance().getnMoveDuration();
            	nlastMoveTime = reachButtonTime - animatorStartTime;
            	if (nlastMoveTime > nMoveDuration) {
            		nlastMoveTime = nMoveDuration;
            	}
            	Ctools.PrintLog("########reachButtonTime:" + reachButtonTime + ",animatorStartTime:" + animatorStartTime );
            	
            	int nMovelen=SSConfig.getInstance().getnMovelen();
            	nRealLastMovelen = (int) (new DecelerateInterpolator().getInterpolation((float)nlastMoveTime/nMoveDuration)*nMovelen)
            			- SSConfig.getInstance().getnMoveOffSet();
            	if (nRealLastMovelen < 0) {
            		nRealLastMovelen = 0;
            	}
            	Ctools.PrintLog("########nRealLastMovelen:" + nRealLastMovelen + ",nlastMoveTime:" + nlastMoveTime + ",(float)nlastMoveTime/nMoveDuration:" + (float)nlastMoveTime/nMoveDuration);
//                nRealLastMovelen-=50;//再减去50的偏移，到底通知有延迟
            }
            //[FR 857010]TCT.NB junqiang.shi 2015.11.12 ADD End
        }
        
        boolean isSuccess = ScreenshotComposer.getInstance().LenCompose1(bmpCurrentScreen, nRealLastMovelen, nbmp2Paddingbottom, nbmp1PaddingTop, bDoneClick);
        if (!isSuccess) {
        	// TODO clean up
        	act.getFloatView().PostHasOutOfMemory();
        }

        //显示图片
        if(bDoneClick)
        {
            Ctools.OpenImageInAPPAndFinished(act);
        }

        return true;
    }

    private boolean SaveForPic(Bitmap bmpCurrentScreen,int nbmp2Paddingbottom,int nbmp1PaddingTop,SuperShotActivity act,boolean bDoneClick)
    {
        Ctools.PrintLog("SaveForPic nbmp2PaddingTop--------------->" + nbmp1PaddingTop);
        Ctools.PrintLog("SaveForPic nbmp2Paddingbottom--------------->" + nbmp2Paddingbottom);
        
        //如果out文件不存在，创建它，退出
    	if (ScreenshotComposer.getInstance().getMergedBitmap() == null) {
    		ScreenshotComposer.getInstance().setMergedBitmap(bmpCurrentScreen);
    		return true;
    	}
        
        boolean isSuccess = ScreenshotComposer.getInstance().TCTcompose2(bmpCurrentScreen,nbmp2Paddingbottom,nbmp1PaddingTop,bDoneClick);
        if (!isSuccess) {
        	// TODO clean up
        	act.getFloatView().PostHasOutOfMemory();
        }
        
        //显示图片
        if(bDoneClick)
        {
            Ctools.OpenImageInAPPAndFinished(act);
        }
        return true;
    }

    //============================================
    private void SendTouchEvent(Object obj)
    {
        EventInfo ei=(EventInfo) obj;
        MotionEvent me= MotionEvent.obtain(ei.lDownTime, SystemClock.uptimeMillis(),ei.nEventType, ei.fDownx,ei.fDowny, 0);
        if(ei.nEventType==MotionEvent.ACTION_MOVE)//检测到底
        {
            if(bHasReachBottom)
            {
                 Ctools.PrintLog("HasReachBottom ,Cancel move");
                 return;
            }
        }
        is.sendPointerSync(me);
    }
}
