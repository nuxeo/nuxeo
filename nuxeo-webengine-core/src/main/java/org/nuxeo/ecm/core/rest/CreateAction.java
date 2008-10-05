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

import org.nuxeo.ecm.webengine.model.WebAction;
import org.nuxeo.ecm.webengine.model.impl.DefaultAction;

/**
 * Create a document
 * <p>
 * Accepts the following methods:
 * <ul>
 * <li> GET - get the form to create the document 
 * <li> POST - create the document (same as POST on a document resource)
 * </ul>
 *  
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@WebAction(name="create", type="Document", guard="WRITE")
public class CreateAction extends DefaultAction {

    @GET
    public Object doGet() {
        return getView();
    }
    
    @POST
    public Object doPost() {
        return ((DocumentObject)prev).doPost();
    }
    
}
