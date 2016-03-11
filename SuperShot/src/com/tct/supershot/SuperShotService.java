package com.tct.supershot;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;

/**
 * Created by Fan on 2015/12/4.
 */
public class SuperShotService extends AccessibilityService {
	
	private static final char ENABLED_ACCESSIBILITY_SERVICES_SEPARATOR = ':';

    @Override
    public void onCreate() {
        super.onCreate();
        android.util.Log.i("==MyTest==", "onCreate()");

        Intent intent = new Intent(this, SuperShotActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        android.util.Log.i("==MyTest==", "onDestroy()");
        
        // TODO finish Activity
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int scrollY = event.getScrollY();
        int maxScrollY = event.getMaxScrollY();
        int fromIndex = event.getFromIndex();
        int toIndex = event.getToIndex();
        int itemCount = event.getItemCount();
        
        if (scrollY == -1 && maxScrollY == -1 && toIndex == -1 && itemCount == -1) {
        	return;
        }
        
        android.util.Log.i("==MyTest==", "onAccessibilityEvent()# type: " + ((event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED)?"scrolled":"adapter") + ", scrollY: " + scrollY + ", maxScrollY: " + maxScrollY + ",fromIndex: " + fromIndex + ", toIndex: " + toIndex + ", itemCount: " + itemCount);
        
        CHelper.bHaveVerticalScrollbar = true;
        
        if (scrollY != -1 && maxScrollY != -1 && scrollY >= maxScrollY) {
        	android.util.Log.i("==MyTest==", "onAccessibilityEvent()# reach end. scroll view");
        	CHelper.bHasReachBottom = true;
        	CHelper.reachButtonTime = event.getEventTime();//System.currentTimeMillis();
        } else if (toIndex != -1 && itemCount != -1 && (toIndex + 1) >= itemCount) {
        	android.util.Log.i("==MyTest==", "onAccessibilityEvent()# reach end. adapter view");
        	CHelper.bHasReachBottom = true;
        	CHelper.reachButtonTime = event.getEventTime();//System.currentTimeMillis();
        }
    }

    @Override
    public void onInterrupt() {

    }
    
    public static void enableService(Context context, boolean enable) {
    	// get service list
    	String strServices = Settings.Secure.getString(context.getContentResolver(),
    			Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
    	String[] services = null;
    	if (strServices != null) {
    		services = strServices.split(String.valueOf(ENABLED_ACCESSIBILITY_SERVICES_SEPARATOR));
    	} else {
    		services = new String[0];
    	}

    	// update service list
    	String name = new ComponentName(context, SuperShotService.class).flattenToString();
    	StringBuilder sb = new StringBuilder();
    	for (String service : services) {
    		if (!name.equals(service)) {
    			sb.append(service);
    			sb.append(ENABLED_ACCESSIBILITY_SERVICES_SEPARATOR);
    		}
    	}
    	if (enable) {
    		sb.append(name);
    		sb.append(ENABLED_ACCESSIBILITY_SERVICES_SEPARATOR);
    	}
    	String finalStrServices = sb.toString();
    	Settings.Secure.putString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, finalStrServices);
    	
    	// update 
    	Settings.Secure.putInt(context.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED, finalStrServices.isEmpty() ? 0 : 1);
    	
    	android.util.Log.i("==MyTest==", "enableService()# enable: " + enable + ", accessibilityServices: " + finalStrServices + ", accessibilityEnabled: " + !finalStrServices.isEmpty());
    }
}
