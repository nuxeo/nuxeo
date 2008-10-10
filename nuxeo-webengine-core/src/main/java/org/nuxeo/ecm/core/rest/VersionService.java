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

package org.nuxeo.ecm.core.rest;

import javax.ws.rs.GET;
import javax.ws.rs.POST;

import org.nuxeo.ecm.webengine.model.WebService;
import org.nuxeo.ecm.webengine.model.impl.DefaultService;

/**
 * Version Service - manage document versions
 * TODO not yet implemented
 * <p>
 * Accepts the following methods:
 * <ul>
 * <li> GET - get the last document version
 * <li> DELETE - delete a version 
 * <li> POST - create a new version
 * </ul>
 *  
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@WebService(name="versions", targetTypes={"Document"}, targetFacets={"Versionable"})
public class VersionService extends DefaultService {
    
    @GET
    public Object getLastVersion() {
        return "versions"; // handled by interceptor
    }

    @POST
    public Object doPost() {
        return null;
    }
  
}
