/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nuxeo.ecm.core.work.api;

import java.io.Serializable;

import javax.management.MXBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;

@MXBean
public class WorkSchedulePath implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static final transient ThreadLocal<Work> enteredLocal = new ThreadLocal<>();

    public static final Log log = LogFactory.getLog(WorkSchedulePath.class);

    protected static Boolean captureStack;

    public static final WorkSchedulePath EMPTY = new WorkSchedulePath();

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

    public static synchronized boolean toggleCaptureStack() {
        captureStack = Boolean.valueOf(!isCaptureStackEnabled());
        return captureStack.booleanValue();
    }

    public static synchronized boolean isCaptureStackEnabled() {
        if (captureStack == null) {
            captureStack = Boolean.valueOf(log.isTraceEnabled()
                    || Boolean.parseBoolean(Framework.getProperty("work.schedule.captureStack", "false")));
            // we don't do the initialization as a static field init because
            // the Framework may not be initialized when the class is loaded
        }
        return captureStack.booleanValue();
    }

    public static void newInstance(Work work) {
        Work entered = enteredLocal.get();
        WorkSchedulePath path = new WorkSchedulePath(entered == null ? EMPTY : entered.getSchedulePath(), work);
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

    public boolean isRoot() {
        return parentPath.isEmpty();
    }

    protected WorkSchedulePath(WorkSchedulePath parent, Work work) {
        parentPath = parent.getPath();
        name = name(work);
        scheduleStackTrace = isCaptureStackEnabled() ? new Trace(parent.scheduleStackTrace) : null;
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

    @Override
    public String toString() {
        return "[parentPath=" + parentPath + ", name=" + name + "]";
    }

}
