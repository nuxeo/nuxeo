package org.nuxeo.ecm.core.work.api;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;


public class WorkSchedulePath implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static final transient ThreadLocal<Work> enteredLocal = new ThreadLocal<Work>();

    public static final Log log = LogFactory.getLog(WorkSchedulePath.class);

    public static boolean captureStackEnabled = Framework.isBooleanPropertyTrue("work.captureScheduleStack") ||
            log.isTraceEnabled();

    public static WorkSchedulePath EMPTY = new WorkSchedulePath();

    protected final String parentPath;

    protected final String name;

    protected final transient Trace scheduleStackTrace;

    public class Trace extends Throwable {

        private static final long serialVersionUID = 1L;

        protected Trace(Trace cause) {
            super(getPath(), cause);
        }

        public WorkSchedulePath path() {
            return WorkSchedulePath.this;
        }
    }




    public static boolean captureScheduleStack(boolean enabled) {
        boolean old = captureStackEnabled;
        captureStackEnabled = enabled;
        return old;
    }

    public static void newInstance(Work work) {
        Work entered = enteredLocal.get();
        WorkSchedulePath  path = new WorkSchedulePath(entered == null ? EMPTY : entered.getSchedulePath(), work);
        work.setSchedulePath(path);
    }

    public static void handleEnter(Work work) {
        if (enteredLocal.get() != null) {
            throw new AssertionError("thread local leak, chain should not be re-rentrant");
        }
        enteredLocal.set(work);
     }

    public static void handleReturn() {
        enteredLocal.remove();
    }

    protected static String path(WorkSchedulePath parent) {
        if (EMPTY.equals(parent)) {
            return "";
        }
        return parent.parentPath + "/" + parent.name;
    }

    protected static String name(Work work) {
        return work.getCategory() + ":" + work.getId();
    }

    protected WorkSchedulePath() {
        parentPath = "";
        name = "";
        scheduleStackTrace = null;
    }

    protected WorkSchedulePath(WorkSchedulePath parent, Work work) {
        parentPath = parent.getPath();
        name = name(work);
        scheduleStackTrace = captureStackEnabled ? new Trace(parent.scheduleStackTrace) : null;
    }

    public String getPath() {
        return path(this);
    }

    public String getParentPath() {
        return parentPath;
    }

    public Trace getStack() {
        return scheduleStackTrace;
    }

}