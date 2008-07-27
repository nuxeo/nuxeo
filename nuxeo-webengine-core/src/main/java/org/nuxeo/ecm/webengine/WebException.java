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

package org.nuxeo.ecm.webengine;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.webengine.exceptions.WebDocumentException;
import org.nuxeo.ecm.webengine.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.servlet.WebConst;

public class WebException extends Exception {

    public static final String ID = "generic";

    private static final long serialVersionUID = 176876876786L;

    private int returnCode = WebConst.SC_INTERNAL_SERVER_ERROR;


    public WebException(String message) {
        super(message);
    }

    public WebException(String message, int code) {
        super(message);
        returnCode = code;
    }

    public WebException(String message, Throwable t) {
        super(message, t);
    }

    public WebException(String message, Throwable t, int code) {
        super(message, t);
        returnCode = code;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public String getId() {
        return ID;
    }


    public static WebException wrap(Throwable e) {
        return wrap(null, e);
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public static WebException wrap(String message, Throwable e) {
        //TODO add EJBAccessException dependency
        if (e instanceof DocumentSecurityException || "javax.ejb.EJBAccessException".equals(e.getClass().getName())) {
            return new WebSecurityException(message, e);
        } else if (e instanceof WebException) {
            return (WebException)e;
        } else if (e instanceof ClientException) {
            Throwable cause = e.getCause();
            if (cause != null && cause.getMessage() != null) {
                if (cause.getMessage().contains("org.nuxeo.ecm.core.model.NoSuchDocumentException")) {
                    return new WebResourceNotFoundException(cause.getMessage(), e);
                }
            }
            return new WebDocumentException(message, (ClientException)e);
        } else {
            return new WebException(message, e);
        }
    }

}
