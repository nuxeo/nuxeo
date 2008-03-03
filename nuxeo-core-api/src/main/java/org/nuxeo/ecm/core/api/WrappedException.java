/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WrappedException extends Exception {

    private static final long serialVersionUID = -8068323167952050687L;

    // the class name of the original exception
    private String className;

    private WrappedException(String message, WrappedException cause) {
        super(message, cause);
    }

    public String getClassName() {
        return className;
    }

    public boolean sameAs(String className) {
        return this.className == null
            ?  className == null : this.className.equals(className);
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public static WrappedException wrap(Throwable t) {
        if (t == null) {
            return null;
        }
        if (t instanceof WrappedException) {
            return (WrappedException) t;
        }
        String exceptionClass = t.getClass().getName();
        String message = "Exception: " + exceptionClass + ". message: "
                + t.getMessage();
        WrappedException cause =  wrap(t.getCause());
        WrappedException we = new WrappedException(message, cause);
        we.className = exceptionClass;
        we.setStackTrace(t.getStackTrace());
        return we;
    }

}
