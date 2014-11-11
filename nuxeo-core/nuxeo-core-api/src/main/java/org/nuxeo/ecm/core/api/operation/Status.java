/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.operation;

import java.io.Serializable;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Status implements Serializable {

    private static final long serialVersionUID = 8300750665351096031L;

    public static final int OK = 0;
    public static final int WARNING = 1;
    public static final int ERROR = 2;
    public static final int CANCEL = 3;

    public static final Status STATUS_OK = new Status(OK, "Ok");

    protected final int severity;
    private final Serializable details;

    public Status(int severity) {
        this.severity = severity;
        details = null;
    }

    public Status(int severity, String message) {
        this.severity = severity;
        details = message;
    }

    public Status(int severity, Throwable exception) {
        this.severity = severity;
        details = exception;
    }

    public int getSeverity() {
        return severity;
    }

    public Serializable getDetails() {
        return details;
    }

    public boolean isOk() {
        return severity == OK;
    }

    public boolean isError() {
        return severity == ERROR;
    }

    public boolean isCancel() {
        return severity == CANCEL;
    }

    public boolean isWarning() {
        return severity == WARNING;
    }

    @SuppressWarnings({"ObjectEquality"})
    public String getMessage() {
        if (details == null) {
            return null;
        }
        return details.getClass() == String.class ? (String) details
                : ((Throwable) details).getMessage();
    }

    public Throwable getException() {
        if (details instanceof Throwable) {
            return (Throwable)details;
        }
        return null;
    }

    /**
     * NOT yet impl.
     * @return
     */
    public boolean isMultiStatus() {
        return false;
    }

}
