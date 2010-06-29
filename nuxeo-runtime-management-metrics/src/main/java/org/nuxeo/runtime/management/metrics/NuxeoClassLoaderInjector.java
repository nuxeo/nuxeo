package org.nuxeo.runtime.management.metrics;

import org.nuxeo.runtime.api.Framework;

public class NuxeoClassLoaderInjector {

    protected static ThreadLocal<ClassLoader> ctx = new ThreadLocal<ClassLoader>();

    public static void replace() {
        if (ctx.get() != null) {
            return;
        }
        ctx.set(Thread.currentThread().getContextClassLoader());
        Thread.currentThread().setContextClassLoader(Framework.class.getClassLoader());
    }

    public static void restore() {
        Thread.currentThread().setContextClassLoader(ctx.get());
        ctx.remove();
    }

}
