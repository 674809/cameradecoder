package com.test.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public  class AutoClickAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
            try {
                //拿到根节点
                AccessibilityNodeInfo rootInfo = getRootInActiveWindow();
                if (rootInfo == null) {
                    return;
                }
                    //开始遍历，这里拎出来细讲，直接往下看正文
                if(rootInfo.getChildCount() !=0){

                }
            }catch (Exception e){
                e.printStackTrace();
            }

    }
    @Override
    public void onInterrupt() {

    }
}
