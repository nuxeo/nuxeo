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

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.webengine.abdera.AbderaService;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@WebObject(type="atomcollection")
public class CollectionResource extends DefaultObject {

    protected CollectionInfo info;

    protected void initialize(Object ... args) {
        this.info = (CollectionInfo)args[0];
    }

    @GET
    public Response getFeed() {
        return AbderaService.getFeed(ctx, info.getCollectionAdapter());
    }

    @POST
    public Response postEntry() {
        return AbderaService.postEntry(ctx, info.getCollectionAdapter());
    }

    public CollectionInfo getCollectionInfo() {
        return info;
    }
}
