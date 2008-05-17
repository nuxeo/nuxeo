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
import org.nuxeo.ecm.webengine.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.servlet.WebConst;

public class WebException extends ClientException {

    private static final long serialVersionUID = 176876876786L;

    public final static String ID = "generic";

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


    public static WebException wrap(Exception e) {
        return wrap(null, e);
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public static WebException wrap(String message, Exception e) {
        if (e instanceof DocumentSecurityException) {
            return new WebSecurityException(message, e);
        } else if (e instanceof ClientException) {
            return new WebDocumentException(message, (ClientException)e);
        } else {
            return new WebException(message, e);
        }
    }

}
