/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.theme.webwidgets.ui;

import javax.ws.rs.core.Response;

import org.nuxeo.ecm.webengine.WebException;

public class WidgetEditorException extends WebException {

    private static final long serialVersionUID = 1L;

    public WidgetEditorException(Throwable cause) {
        super(cause);
    }

    public WidgetEditorException(String message) {
        super(message);
    }

    public WidgetEditorException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public Response getResponse() {
        return Response.status(500).entity(this.getMessage()).build();
    }

}
