package com.hua.leanwms.wms;

import java.lang.reflect.Method;

/**
 * @author zhangsh
 * @version V1.0
 * @date 2019-12-01 11:40
 */

public class MyObject {

    public Object object;
    private MyClass myClass;

    public MyObject(MyClass myClass, Object obj) {
        this.myClass = myClass;
        object = obj;
    }

    Object invokeMethod(String name, Object... params) throws Exception {
        Method method = myClass.getMethod(name, params);
        return method.invoke(this.object, params);
    }

}
