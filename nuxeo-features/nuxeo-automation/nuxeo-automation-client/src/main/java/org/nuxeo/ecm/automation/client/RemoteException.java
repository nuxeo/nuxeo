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
 */
package org.nuxeo.ecm.automation.client;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class RemoteException extends AutomationException {

    private static final long serialVersionUID = 1L;

    protected final int status;

    protected final String type;
    
    protected final String info;
    
    protected final Throwable remoteCause;
    
    public RemoteException(int status, String type, String message,
            Throwable cause) {
        super(message, cause);
        this.status = status;
        this.type = type;
        this.info = extractInfo(cause);
        this.remoteCause = cause;
    }

    public RemoteException(int status, String type, String message, String info) {
        super(message);
        this.status = status;
        this.type = type;
        this.info = info;
        this.remoteCause = null;
    }

    public int getStatus() {
        return status;
    }

    public String getType() {
        return type;
    }

    protected static String extractInfo(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.getBuffer().toString();
    }
    
    public Throwable getRemoteCause() {
        return remoteCause;
    }

    public String getRemoteStackTrace() {
        return status + " - " + getMessage() + "\n" + info;
    }

    @Override
    public void printStackTrace(PrintStream s) {
        super.printStackTrace(s);
        s.println("====== Remote Stack Trace:");
        s.print(getRemoteStackTrace());
    }

    @Override
    public void printStackTrace(PrintWriter s) {
        super.printStackTrace(s);
        s.println("====== Remote Stack Trace:");
        s.print(getRemoteStackTrace());
    }

    public static RemoteException wrap(Throwable t) {
        return wrap(t, 500);
    }

    public static RemoteException wrap(Throwable t, int status) {
        return wrap(t.getMessage(), t, status);
    }

    public static RemoteException wrap(String message, Throwable t) {
        return wrap(message, t, 500);
    }

    public static RemoteException wrap(String message, Throwable t, int status) {
        RemoteException e = new RemoteException(status, t.getClass().getName(), message, t);
        e.initCause(t);
        return e;
    }

}
