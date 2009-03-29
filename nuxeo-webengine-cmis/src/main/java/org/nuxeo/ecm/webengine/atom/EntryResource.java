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
package org.nuxeo.ecm.webengine.atom;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Response;

import org.apache.abdera.protocol.server.CollectionAdapter;
import org.nuxeo.ecm.webengine.abdera.AbderaService;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@WebObject(type="atomentry")
public class EntryResource extends DefaultObject {   
    
    protected AbderaService service;
    protected CollectionAdapter adapter; 
        
    protected void initialize(Object ... args) {
        this.adapter = (CollectionAdapter)args[0];
    }
            
    @GET
    public Response getEntry() {
        return AbderaService.getEntry(ctx, adapter);
    }
    
    @PUT
    public Response putEntry() {
        return AbderaService.putEntry(ctx, adapter);
    }
    
    @DELETE
    public Response deleteEntry() {
        return AbderaService.deleteEntry(ctx, adapter);
    }

    @HEAD
    public Response headEntry() {
        return AbderaService.headEntry(ctx, adapter);
    }
    
//TODO define @OPTIONS annotation    
//    @OPTIONS
//    public Response optionsEntry() {
//        return AbderaService.optionsEntry(ctx, adapter);
//    }
    
}
