/*******************************************************************************
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.nuxeo.ecm.core.work.api;

import java.io.Serializable;

import javax.management.MXBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;


@MXBean
public class WorkSchedulePath implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static final transient ThreadLocal<Work> enteredLocal = new ThreadLocal<Work>();

    public static final Log log = LogFactory.getLog(WorkSchedulePath.class);

    public static boolean capturePath = Boolean.parseBoolean(Framework.getProperty("work.schedule.capturePath", "true"));

    public static boolean captureStack = Boolean.parseBoolean(Framework.getProperty("work.schedule.captureStack", "false")) ||
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

    public static void toggleCapturePath() {
        capturePath = !capturePath;
    }

    public static boolean isCapturePathEnabled() {
        return capturePath;
    }

    public static void toggleCaptureStack() {
        captureStack = !captureStack;
        if (captureStack) {
            capturePath = true;
        }
    }

    public static boolean isCaptureStackEnabled() {
        return capturePath && captureStack;
    }

    public static void newInstance(Work work) {
        if (!capturePath) {
            return;
        }
        Work entered = enteredLocal.get();
        WorkSchedulePath  path = new WorkSchedulePath(entered == null ? EMPTY : entered.getSchedulePath(), work);
        work.setSchedulePath(path);
    }

    public static void handleEnter(Work work) {
        if (!capturePath) {
            return;
        }
        if (enteredLocal.get() != null) {
            throw new AssertionError("thread local leak, chain should not be re-rentrant");
        }
        enteredLocal.set(work);
     }

    public static void handleReturn() {
        if (!capturePath) {
            return;
        }
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
        scheduleStackTrace = captureStack ? new Trace(parent.scheduleStackTrace) : null;
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