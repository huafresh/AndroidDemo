package com.hua.leanwms.wms;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;

/**
 * @author zhangsh
 * @version V1.0
 * @date 2019-12-01 10:18
 */

public class SimpleWindow {

    public static void main() {

        // startInThread();
        try {
            new SimpleWindow().run(false);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void startInThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new SimpleWindow().run(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private Object mWindow;
    private Rect mOutFrame = new Rect();
    private Rect moutContentInsets = new Rect();
    private Rect mOutOverscanInsets = new Rect();
    private Rect outVisibleInsets = new Rect();
    private Configuration mConfig = new Configuration();
    private Surface mSurface;
    private Object mInputChannel;

    private void run(boolean thread) throws Exception {
        if (thread) {
            Looper.prepare();
        }

        // 获取WMS服务
        MyClass smClass = new MyClass("android.os.ServiceManager");
        MyClass stubClass = new MyClass("android.view.IWindowManager$Stub");
        Object wmBinder = smClass.invokeStaticMethod("getService", Context.WINDOW_SERVICE);
        Object wmObj = stubClass.invokeStaticMethod("asInterface", wmBinder);
        MyObject wm = new MyObject(new MyClass("android.view.IWindowManager"), wmObj);

        // 获取session对象
        MyClass wmGlobal = new MyClass("android.view.WindowManagerGlobal");
        Object sessionObj = wmGlobal.invokeStaticMethod("getWindowSession");
        MyObject session = new MyObject(new MyClass("android.view.IWindowSession"), sessionObj);

        IBinder token = new Binder();
        // wm.invokeMethod("addWindowToken", token, WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        WindowManager.LayoutParams params = getLayoutParams(token);
        session.invokeMethod("add", getMyWindow(session.object), 0, params,
                View.VISIBLE, moutContentInsets, getInputChannel());
        Surface surface = getSurface();
        session.invokeMethod("relayout", getMyWindow(session.object), 0, params,
                params.width, params.height, View.VISIBLE, 0, mOutFrame, mOutOverscanInsets, moutContentInsets, outVisibleInsets,
                mConfig, surface);

//        (android.view.IWindow window, int seq, android.view.WindowManager.LayoutParams attrs,
//        int requestedWidth, int requestedHeight, int viewVisibility,
//        int flags, android.graphics.Rect outFrame, android.graphics.Rect outOverscanInsets, a
//        ndroid.graphics.Rect outContentInsets, android.graphics.Rect outVisibleInsets, android.content.res.Configuration outConfig, android.view.Surface outSurface) throws android.os.RemoteException;


        if (!surface.isValid()) {
            throw new RuntimeException("surface create failed");
        }

        Canvas canvas = surface.lockCanvas(null);
        canvas.drawColor(Color.parseColor("#D81B60"));
        surface.unlockCanvasAndPost(canvas);
        session.invokeMethod("finishDrawing", getMyWindow(session.object));

        if (thread) {
            Looper.loop();
        }
    }

    private Object getMyWindow(Object session) throws Exception {
        if (mWindow == null) {
            MyClass myClass = new MyClass("com.android.internal.view.BaseIWindow");
            MyObject myObject = myClass.newInstance();
            myObject.invokeMethod("setSession", session);
            mWindow = myObject.object;
        }
        return mWindow;
    }

    private Object getInputChannel() throws Exception {
        if (mInputChannel == null) {
            MyClass myClass = new MyClass("android.view.InputChannel");
            MyObject myObject = myClass.newInstance();
            mInputChannel = myObject.object;
        }
        return mInputChannel;
    }

    private Surface getSurface() throws Exception {
        if (mSurface == null) {
            MyClass myClass = new MyClass("android.view.Surface");
            MyObject myObject = myClass.newInstance();
            mSurface = (Surface) myObject.object;
        }
        return mSurface;
    }

    private WindowManager.LayoutParams getLayoutParams(IBinder token) {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        // params.token = token;
        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        params.setTitle("SimpleWindow");
        params.gravity = Gravity.START | Gravity.TOP;
        params.x = 200;
        params.y = 200;
        params.width = 300;
        params.height = 300;
        params.flags = params.flags | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        return params;
    }


}
