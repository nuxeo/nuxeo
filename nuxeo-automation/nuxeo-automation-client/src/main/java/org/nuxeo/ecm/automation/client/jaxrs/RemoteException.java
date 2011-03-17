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
package org.nuxeo.ecm.automation.client.jaxrs;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class RemoteException extends AutomationException {

    private static final long serialVersionUID = 1L;

    protected final int status;

    protected final String type;

    protected final String stackTrace;

    public RemoteException(int status, String type, String message,
            String stackTrace) {
        super(message);
        this.status = status;
        this.type = type;
        this.stackTrace = stackTrace;
    }

    public int getStatus() {
        return status;
    }

    public String getType() {
        return type;
    }

    public String getRemoteStackTrace() {
        return status + " - " + getMessage() + "\n" + stackTrace;
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
}
