package org.nuxeo.runtime.jboss.interceptors;

import java.lang.reflect.Method;

import org.javasimon.Counter;
import org.javasimon.SimonManager;
import org.javasimon.Stopwatch;
import org.jboss.aop.joinpoint.MethodInvocation;

public abstract class AbstractMonitoring {

    public static final String RESPONSE_DURATION_TAG = "nx:duration";

    protected Stopwatch getStopwatch(MethodInvocation ctx, String ...names) {
        String name = formatName(ctx,names);
        Stopwatch stopwatch = SimonManager.getStopwatch(name);
        stopwatch.setNote(formatNote(ctx));
        return stopwatch;
    }


    protected  Counter getCounter(MethodInvocation ctx, String... names) {
        String name = formatName(ctx, names);
        Counter counter = SimonManager.getCounter(name);
        return counter;
    }

    protected String formatParms(Object ...parms) {
        if (parms == null) {
            return "";
        }
        StringBuffer buffer = new StringBuffer();
        for (Object parm:parms) {
            buffer.append(".").append(parm);
        }
        return buffer.toString();
    }

    protected  String formatName(MethodInvocation context, String ...parms) {
        Method m = context.getActualMethod();
        Class<?> declaringClass = m.getDeclaringClass();
        return String.format("%s.%s%s", declaringClass.getSimpleName(), m.getName(),formatParms(parms));
    }

    protected  String formatNote(MethodInvocation context) {
        Method m = context.getActualMethod();
        return String.format("%s#%s(%s)", m.getDeclaringClass().getSimpleName(), m.getName(), formatParms(context.getArguments()));
    }

}
