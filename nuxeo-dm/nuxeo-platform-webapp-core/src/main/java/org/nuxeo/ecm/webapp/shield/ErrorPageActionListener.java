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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.shield;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;

import org.jboss.seam.Seam;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import  org.jboss.seam.faces.Redirect;

/**
 * Seam component to handle rendering of error pages.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
@Name("errorPageActionListener")
@Scope(SESSION)
@Startup
public class ErrorPageActionListener implements Serializable {

    private static final long serialVersionUID = -1467366100627478101L;

    @In(required = false)
    protected Throwable applicationException;

    public String getStackTrace() {
        StringBuilder sb = new StringBuilder();

        if (applicationException != null) {
            // TODO: refactor to use only one type of security exception
            // everywhere
            Throwable cause = applicationException.getCause();
            if (cause instanceof SecurityException) {
                sb.append(getStackTrace(cause));
                cause = cause.getCause();
            } else {
                sb.append(getStackTrace(applicationException));
            }

            if (cause != null) {
                do {
                    sb.append(getStackTrace(cause));
                } while (null != (cause = cause.getCause()));
            }
        }

        return sb.toString();
    }

    protected static String getStackTrace(Throwable t) {
        StringBuilder sb = new StringBuilder();

        if (null != t) {
            sb.append("\n\n");
            sb.append(t.getClass().getName());
            sb.append("\n\n");
            sb.append(t.getLocalizedMessage());
            sb.append("\n\n");

            for (StackTraceElement element : t.getStackTrace()) {
                sb.append(element.toString());
                sb.append('\n');
            }
        }

        return sb.toString();
    }

    public static void toLoginPage() {
        Seam.invalidateSession();
        Redirect.instance().setViewId("/login.jsp");
        Redirect.instance().execute();
    }

}
