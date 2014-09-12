/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.server.jaxrs.adapters;

import javax.ws.rs.GET;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

/**
 *
 *
 * @since 5.7.3
 */
@WebAdapter(name = "acl", type = "aclAdapter", targetType = "document")
public class ACLAdapter extends DefaultAdapter {

    @GET
    public ACP doGetACP() throws ClientException {
        DefaultObject target = (DefaultObject) getTarget();
        DocumentModel doc = target.getAdapter(DocumentModel.class);

        if(doc == null) {
            throw new IllegalArgumentException("Targeted resource should be adaptable to a document");
        }

        return doc.getACP();
    }
}
