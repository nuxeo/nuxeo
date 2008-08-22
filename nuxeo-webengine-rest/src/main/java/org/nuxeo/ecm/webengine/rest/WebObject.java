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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.rest;

import javax.ws.rs.ProduceMime;

import org.nuxeo.ecm.webengine.rest.types.WebType;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 *There is a bug in ResourceJavaMethodDispatcher#getAcceptableMediaType
 * when no setting the mime type it will return binary content ...
 */
@ProduceMime({"text/html", "*/*"})
public abstract class WebObject {

    protected String path;

    public WebObject(String path) {
        this.path = path;
    }

    /**
     * @return the path.
     */
    public String getPath() {
        return path;
    }

    public WebType getType() {
        return WebType.OBJECT;
    }

}
