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

package org.nuxeo.ecm.webengine.rest.domains;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebObject;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebDomain {

    protected String id;
    protected WebEngine engine;

    public WebDomain() {
        try {
            this.engine = Framework.getService(WebEngine.class);
        } catch (Exception e) {
            throw new Error("Failed to get WebEngine service", e);
        }
    }

    public String getId() {
        return id;
    }

    protected WebObject resolve(String path) {
        return null;
    }

    @Path(value="{path}", limited=false)
    public WebObject dispatch(@PathParam("path") String path) {
        return resolve(path);
    }

}
