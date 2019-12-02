package com.hua.leanwms.wms;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author zhangsh
 * @version V1.0
 * @date 2019-12-01 11:03
 */

public class MyClass {

    public Class<?> mClass;

    public MyClass(String name) {
        try {
            mClass = Class.forName(name);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    MyObject newInstance(Object... params) throws Exception {
        List<List<Class>> possibleArgs = getPossibleArgList(params);

        Constructor<?> constructor = null;
        for (List<Class> possibleArg : possibleArgs) {
            boolean fond = true;
            try {
                constructor = mClass.getConstructor(possibleArg.toArray(new Class[]{}));
                constructor.setAccessible(true);
            } catch (NoSuchMethodException e) {
                fond = false;
            }
            if (fond) {
                break;
            }
        }
        if (constructor == null) {
            constructor = mClass.getConstructor();
        }
        return new MyObject(this, constructor.newInstance(params));
    }

    Object invokeStaticMethod(String name, Object... params) throws Exception {
        return getMethod(name, params).invoke(null, params);
    }

    Method getMethod(String name, Object... params) throws Exception {
        List<List<Class>> possibleArgs = getPossibleArgList(params);

        Method method = null;
        for (List<Class> possibleArg : possibleArgs) {
            boolean fond = true;
            try {
                method = mClass.getDeclaredMethod(name, possibleArg.toArray(new Class[]{}));
                method.setAccessible(true);
            } catch (NoSuchMethodException e) {
                fond = false;
            }
            if (fond) {
                break;
            }
        }
        if (method == null && params.length == 0) {
            method = mClass.getDeclaredMethod(name);
        }
        return method;
    }

    private List<List<Class>> getPossibleArgList(Object[] params) {
        // 存储所有可能的参数组合，每一项都会尝试作为getDeclaredMethod的参数获取Method对象。
        List<List<Class>> possibleArgs;
        // 存储可变参数params转List<Class>的结果，因为某一个参数可能存在多态，因此嵌套了一个List
        List<List<Class>> paramsClass = new ArrayList<List<Class>>();

        for (Object param : params) {
            List<Class> list = new ArrayList<Class>();
            Class tempClass = param.getClass();
            if (tempClass == Integer.class) {
                list.add(int.class);
            } else {
                do {
                    list.add(tempClass);
                    Class[] interfaces = tempClass.getInterfaces();
                    Collections.addAll(list, interfaces);
                    tempClass = tempClass.getSuperclass();
                } while (tempClass != Object.class);
            }
            paramsClass.add(list);
        }

        // 接下来要把paramsClass转成possibleArgs，算法思路是递归暴力遍历，理解的时候参考动态规划的思想。
        possibleArgs = toPossibleArgs(paramsClass);
        return possibleArgs;
    }

    private static List<List<Class>> toPossibleArgs(List<List<Class>> paramClass) {
        List<List<Class>> result = new ArrayList<List<Class>>();
        toPossibleArgsRecursive(paramClass, 0, result);
        return result;
    }

    private static void toPossibleArgsRecursive(List<List<Class>> paramClass, int index, List<List<Class>> result) {
        if (index == paramClass.size()) {
            return;
        }

        int curSize = result.size();
        List<List<Class>> cloneList = new ArrayList<List<Class>>(result);
        if (curSize == 0) {
            for (Class clazz : paramClass.get(index)) {
                List<Class> newList = new ArrayList<Class>();
                newList.add(clazz);
                result.add(newList);
            }
        } else {
            // 先指数扩容result
            List<Class> paramList = paramClass.get(index);
            for (int i = 1; i < paramList.size(); i++) {
                // 这里需要注意浅拷贝的影响
                for (List<Class> tempList : cloneList) {
                    List<Class> copyList = new ArrayList<Class>(tempList);
                    result.add(copyList);
                }
                // result.addAll(new ArrayList<List<Class>>(cloneList));
            }

            // 然后在此基础上，每一种情况都要追加目前的clazz
            for (int i = 0; i < paramList.size(); i++) {
                Class addClass = paramList.get(i);
                for (int j = 0; j < curSize; j++) {
                    result.get(i * curSize + j).add(addClass);
                }
            }
        }

        toPossibleArgsRecursive(paramClass, index + 1, result);
    }

}
