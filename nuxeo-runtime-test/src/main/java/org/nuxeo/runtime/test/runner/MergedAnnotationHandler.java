package org.nuxeo.runtime.test.runner;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

public class MergedAnnotationHandler<T extends Annotation> implements InvocationHandler {

    public static <T extends Annotation> T newProxy(List<T> requestedValues, Class<T> itf) {
        InvocationHandler h = new MergedAnnotationHandler<T>(requestedValues, itf);
        return itf.cast(Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[] { itf }, h));
    }

    protected MergedAnnotationHandler(List<T> values, Class<T> itf) {
        defaultValue = Defaults.of(itf);
        requestedValues = values;
    }

    T defaultValue;

    List<T> requestedValues;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        Object d = method.invoke(defaultValue, args);
        Object v = d;
        for (T requestedValue:requestedValues) {
            Object r = method.invoke(requestedValue, args);
            if (!d.equals(r)) {
                v = r;
            }
        }
        return v;
    }


}
