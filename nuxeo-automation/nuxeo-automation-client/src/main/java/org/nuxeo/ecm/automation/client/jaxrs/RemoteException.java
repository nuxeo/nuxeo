/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
