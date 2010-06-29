package org.nuxeo.ecm.core.storage.sql.management;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.nuxeo.ecm.core.storage.sql.Mapper;

public class MonitoringMapper implements InvocationHandler {

    protected Mapper proxied;

    protected MonitoringMapper(Mapper mapper) {
        this.proxied = mapper;
    }

    public static Mapper newProxy(Mapper mapper) {
        MonitoringMapper handler = new MonitoringMapper(mapper);
        return (Mapper) Proxy.newProxyInstance(Mapper.class.getClassLoader(), new Class<?>[] { Mapper.class }, handler);
    }

    protected String formatParms(Object... parms) {
        if (parms == null) {
            return "";
        }
        StringBuffer buffer = new StringBuffer();
        for (Object parm : parms) {
            buffer.append(".").append(parm);
        }
        return buffer.toString();
    }

    protected String formatName(Method m, Object[] parms) {
        Class<?> declaringClass = m.getDeclaringClass();
        return String.format("%s.%s", declaringClass.getSimpleName(), m.getName());
    }

    protected String formatNote(Method m, Object[] parms) {
        return String.format("%s#%s(%s)", m.getDeclaringClass().getSimpleName(), m.getName(), formatParms(parms));
    }

    protected Stopwatch getStopwatch(Method m, Object[] parms) {
        String name = formatName(m, parms);
        Stopwatch stopwatch = SimonManager.getStopwatch(name);
        stopwatch.setNote(formatNote(m, parms));
        return stopwatch;
    }

    public Object invoke(Object proxy, Method m, Object[] parms) throws Throwable {
        Split split = getStopwatch(m, parms).start();
        try {
            return m.invoke(proxied, parms);
        } finally {
            split.stop();
        }

    }

}
