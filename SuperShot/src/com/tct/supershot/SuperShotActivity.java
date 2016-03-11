package com.tct.supershot;

import com.tct.supershot.model.SSConfig;

import info.papdt.pano.processor.ScreenshotComposer.ProgressListener;
import android.app.Activity;
import android.app.StatusBarManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

public class SuperShotActivity extends Activity  implements ProgressListener {
	private static final int MAX_SCROLL_TIME = 12;
	private static final boolean ENABLE_LENGTH_COMPOSE = false;
	
    private WindowManager mWindowManager;
    private FloatView mLayout;
    private StatusBarManager sbm;
    private ClickListener clklistener;

    private CHelper chelper;
    
    private int mScrolledTimes = 0;
    
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        
        // start accessibility service
        if (ENABLE_LENGTH_COMPOSE) {
        	SuperShotService.enableService(this, true);
        }

        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        Display mDisplay = mWindowManager.getDefaultDisplay();
        DisplayMetrics mDisplayMetrics = new DisplayMetrics();
        mDisplay.getRealMetrics(mDisplayMetrics);
        int nScreenWidth=mDisplayMetrics.widthPixels;
        int nScreenHeight=mDisplayMetrics.heightPixels;
        int nScreenRotation=mWindowManager.getDefaultDisplay().getRotation();
        int nStatusbarHeight=Ctools.GetStatusBarH(this);

        int mTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        int nMoveOffSet = mTouchSlop;//移动偏移量
        int nMovelen=-1,nBottomPadding=-1;
        if(nScreenHeight<nScreenWidth)
        {
            nMovelen=500;//移动距离
            nBottomPadding=200;//触屏落点
        }
        else
        {
            nMovelen=1000;
            nBottomPadding=200;
        }

        SSConfig.init(nScreenWidth, nScreenHeight, nScreenRotation, nStatusbarHeight,
                nMovelen, nBottomPadding, nMoveOffSet,300);
        chelper=new CHelper();

        clklistener=new ClickListener()
        {
            @Override
            public void ScrollDownclick()
            {
                chelper.ScrollDown(SuperShotActivity.this, mLayout, false);
                mScrolledTimes ++;
                if (mScrolledTimes > MAX_SCROLL_TIME) {
                	mLayout.PostReachScrollLimit();
                }
            }

            @Override
            public void Done_click()
            {
                if(Ctools.bNoCaptured())
                {
                    return;
                }
                android.util.Log.i("==MyTest==", "Done");
                mLayout.disableAllButton();

                chelper.ScrollDown(SuperShotActivity.this, mLayout, true);
            }

            @Override
            public void Cancle_click()
            {
                SuperShotActivity.this.finish();
            }
        };

        mLayout= new FloatView(this.getApplicationContext(),clklistener);
        mWindowManager.addView(mLayout,mLayout.getLayoutParams());

        //实现点击穿透
        LayoutParams layp= getWindow().getAttributes();
        layp.flags = layp.flags| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;//实现点击穿透！！！

        //禁止下拉状态栏
        sbm=((StatusBarManager)getSystemService(Context.STATUS_BAR_SERVICE));
        sbm.disable(StatusBarManager.DISABLE_EXPAND);

        //禁止弹出输入法
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
	protected void onDestroy() {
        //恢复状态栏下拉
        sbm.disable(StatusBarManager.DISABLE_NONE);

        chelper.clean();
        
        if (ENABLE_LENGTH_COMPOSE) {
            SuperShotService.enableService(this, false);
        }

		super.onDestroy();
	}

    @Override
	public void finish() {
    	mWindowManager.removeView(mLayout);
		super.finish();
	}

	public void setInfo(String szMsg)
    {
        mLayout.setInfo(szMsg);
    }

    public FloatView getFloatView()
    {
        return mLayout;
    }

    /**
     * ProgressListener 接口的方法实现
     */
    @Override
    public void onAnalyzingImage(int i, int j, int total)
    {
        mLayout.setInfo("AnalyzingImage: "+(i+1)+" / "+total);
        Ctools.PrintLog("=====cur:"+i+"/next:"+j+"/total:"+total+"=====");
    }

    /**
     * ProgressListener 接口的方法实现
     */
    @Override
    public void onComposingImage()
    {
        mLayout.setInfo("Composing Image");
        Ctools.PrintLog("=====onComposingImage=====");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Ctools.PrintLog("main=====> keyCode : "+keyCode);
        return super.onKeyDown(keyCode, event);
    }

    public Handler getHandler() {
    	return mHandler;
    }
}
