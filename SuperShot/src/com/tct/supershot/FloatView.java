package com.tct.supershot;

import com.tct.supershot.model.SSConfig;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class FloatView extends RelativeLayout
{
    private final int nSel_Accuracy=50;//手指选择精度，误差
    private float fDownx=-1;//点击时横坐标
    private float fDowny=-1;//点击时纵坐标

    private ClickListener cl;
    private Rect rSelectRect=new Rect();
    private Button btnScrollDown;
    private Button btnDone;
    private Button btnCancle;
    private TextView txtInfo;

    private int mSelectLineY = -1;
    public FloatView(Context con, ClickListener clk)
    {
        super(con);

        cl=clk;

        WindowManager.LayoutParams param = new WindowManager.LayoutParams();

        setBackgroundColor(android.R.color.holo_red_dark);

        // 设置LayoutParams(全局变量）相关参数
        param.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT; // 系统提示类型,重要
        param.format = 1;
        //param.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE; // 不能抢占聚焦点
        //param.flags = param.flags| WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        param.flags = param.flags| WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        param.flags = param.flags| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN; // 排版不受限制

        //param.flags = param.flags| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;//实现点击穿透！！！
        param.alpha = 1.0f;

        param.gravity = Gravity.LEFT | Gravity.TOP; // 调整悬浮窗口至左上角
        // 以屏幕左上角为原点，设置x、y初始值
        param.x = 0;
        param.y = 0;

        // 设置悬浮窗口长宽数据
        param.width = WindowManager.LayoutParams.MATCH_PARENT;
        param.height = WindowManager.LayoutParams.MATCH_PARENT;//WRAP_CONTENT;

        setLayoutParams(param);

        View vFromLayout=inflate(getContext(), R.layout.floatview, null);
        btnScrollDown=(Button) vFromLayout.findViewById(R.id.btnSrollDown);
        btnDone=(Button) vFromLayout.findViewById(R.id.btnDone);
        btnCancle=(Button) vFromLayout.findViewById(R.id.btnCancle);
        txtInfo=(TextView) vFromLayout.findViewById(R.id.info);
        addView(vFromLayout);

        btnScrollDown.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                cl.ScrollDownclick();
            }
        });

        btnDone.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                cl.Done_click();
            }
        });

        btnCancle.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                cl.Cancle_click();
            }
        });

        ResetSelectRect();

        setOnKeyListener(new View.OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                Ctools.PrintLog("=====> keyCode : "+keyCode);
                return false;
            }
        });

    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        fDownx=event.getX();
        fDowny=event.getY();
        int nAction=event.getAction();
        switch (nAction)
        {
            case MotionEvent.ACTION_DOWN:
                GetNearestLine(fDownx, fDowny);
                break;
            case MotionEvent.ACTION_MOVE:
                if(mSelectLineY!=-1)
                {
                    int nScreenHeight=SSConfig.getInstance().getnScreenHeight();

                    if(mSelectLineY==rSelectRect.top)
                    {
                        if(fDowny+5*nSel_Accuracy<nScreenHeight/2)
                        {
                            rSelectRect.top=(int) fDowny;
                            mSelectLineY=rSelectRect.top;
                        }
                    }
                    else
                    {
                        if((fDowny-5*nSel_Accuracy>nScreenHeight/2)&&fDowny<=nScreenHeight)
                        {
                            Ctools.PrintLog("move bottom:"+fDowny);
                            rSelectRect.bottom=(int) fDowny;
                            mSelectLineY=rSelectRect.bottom;
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mSelectLineY=-1;
                fDownx=-1;
                break;
            default:
                return super.onTouchEvent(event);
        }
        postInvalidate();
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        //Cutils.PrintLog("dispatchTouchEvent:"+ev);
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        //Cutils.PrintLog("onInterceptTouchEvent:"+ev);
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        Ctools.PrintLog("keyCode:"+keyCode);
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 返回此刻选框矩阵
     * @return
     */
    public Rect getSelectRect()
    {
        Ctools.PrintLog("getSelectRect -- "+rSelectRect);
        return rSelectRect;
    }

    private void ResetSelectRect()
    {
        int nStatusbarHeight=SSConfig.getInstance().getnStatusbarHeight();
        int nScreenWidth=SSConfig.getInstance().getnScreenWidth();
        int nScreenHeight=SSConfig.getInstance().getnScreenHeight();
        rSelectRect.set(0, nStatusbarHeight, nScreenWidth, nScreenHeight);

        postInvalidate();
        Ctools.PrintLog("ResetSelectRect -- "+rSelectRect);
    }

    public Rect getDefaultRect()
    {
        int nStatusbarHeight=SSConfig.getInstance().getnStatusbarHeight();
        int nScreenWidth=SSConfig.getInstance().getnScreenWidth();
        int nScreenHeight=SSConfig.getInstance().getnScreenHeight();

        return new Rect(0, nStatusbarHeight, nScreenWidth, nScreenHeight);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        Paint p=new Paint();
        p.setStrokeWidth(10);
        p.setColor(Color.BLUE);
        canvas.drawLine(rSelectRect.left, rSelectRect.top, rSelectRect.left, rSelectRect.bottom, p);
        canvas.drawLine(rSelectRect.left, rSelectRect.top, rSelectRect.right, rSelectRect.top, p);
        canvas.drawLine(rSelectRect.right, rSelectRect.top, rSelectRect.right, rSelectRect.bottom, p);
        canvas.drawLine(rSelectRect.left, rSelectRect.bottom, rSelectRect.right, rSelectRect.bottom, p);

        if(Ctools.bNoCaptured())//没截屏过，可以拖拽上边界
        {
            canvas.drawCircle(rSelectRect.right/2, rSelectRect.top, 20, p);
        }
        else //截过屏才能拖拽下边界
        {
            canvas.drawCircle(rSelectRect.right/2, rSelectRect.bottom, 20, p);
        }

        //绘制手指落点
        if(fDownx!=-1)
        {
            canvas.drawCircle(fDownx, fDowny, nSel_Accuracy, p);
        }
    }

    /**
     * 根据当前的落点，选择框边界
     * @param x
     * @param y
     */
    private void GetNearestLine(float x,float y)
    {
        if(!btnDone.isEnabled())//已经点击了完成按钮
        {
            return;
        }
        mSelectLineY=-1;
        int nStatusbarHeight=SSConfig.getInstance().getnStatusbarHeight();
        int nOffset=rSelectRect.top<nStatusbarHeight?nStatusbarHeight:0;//窗口最顶端的位置，触屏事件会传递给状态栏，所以要加偏移量以便选中上边界
        Rect Top=new Rect(rSelectRect.left, rSelectRect.top-nSel_Accuracy+nOffset, rSelectRect.right, rSelectRect.top+nSel_Accuracy+nOffset);

        Rect Bottom=new Rect(rSelectRect.left, rSelectRect.bottom-nSel_Accuracy, rSelectRect.right, rSelectRect.bottom+nSel_Accuracy);

        if(Ctools.bNoCaptured()&&Top.contains((int)x,(int)y))
        {
            mSelectLineY=rSelectRect.top;
            Ctools.PrintLog("y:"+y+"  top:"+rSelectRect.top);
        }
        else if(!Ctools.bNoCaptured()&&Bottom.contains((int)x,(int)y))
        {
            mSelectLineY=rSelectRect.bottom;
            //Cutils.PrintLog("y:"+y+"  bottom:"+rSelectRect.bottom);
            btnScrollDown.setEnabled(false);
        }
        else
        {
            //Cutils.PrintLog("y:"+y+"  none.");
        }
    }


    //============================================
    public void TemporaryHide(boolean bValue)
    {
        setVisibility(bValue?View.INVISIBLE:View.VISIBLE);
        if(!bValue)
        {
            ResetSelectRect();
        }
    }

     public void PostTemporaryHide(final boolean bValue)
     {
         post(new Runnable()
         {
            @Override
            public void run()
            {
                TemporaryHide(bValue);
            }
        });
     }

     public void setInfo(final String info)
     {
         txtInfo.setText(info);
         txtInfo.setVisibility(View.VISIBLE);
     }

     public void setInfo(int resId) {
    	 txtInfo.setText(resId);
         txtInfo.setVisibility(View.VISIBLE);
     }

     public void PostSetInfo(final int nValue)
     {
         post(new Runnable() {

            @Override
            public void run() {
                setInfo(getResources().getString(nValue));
            }
        });
     }

     public void PostHasReachBottom()
     {
         post(new Runnable() {

             @Override
             public void run() {
                 setInfo(getContext().getString(R.string.reachbottom));
                 btnScrollDown.setEnabled(false);
             }
         });

     }
     
     public void PostHasOutOfMemory()
     {
         post(new Runnable() {

             @Override
             public void run() {
                 setInfo(getContext().getString(R.string.outofmemory));
                 btnScrollDown.setEnabled(false);
             }
         });

     }

     public void PostReachScrollLimit()
     {
         post(new Runnable() {

             @Override
             public void run() {
                 setInfo(getContext().getString(R.string.reach_scroll_limit));
                 btnScrollDown.setEnabled(false);
             }
         });

     }

     public void disableAllButton()
    {
        btnScrollDown.setEnabled(false);
        btnDone.setEnabled(false);
        btnCancle.setEnabled(false);
    }
}