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
package org.nuxeo.ecm.automation.rest.jaxrs.adapters;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;
import org.nuxeo.runtime.api.Framework;

/**
 *
 *
 * @since 5.7.2
 */
@WebAdapter(name = "children", type = "ChildrenService")
@Produces({ "application/json+nxentity", MediaType.APPLICATION_JSON })
public class ChildrenAdapter extends DefaultAdapter {

    @Override
    public <A> A getAdapter(Class<A> adapter) {
        if (adapter.isAssignableFrom(DocumentModelList.class)) {
            try {
                return (A) getDocs(0L, 50L);
            } catch (ClientException e) {
                return null;
            }

        }
        return super.getAdapter(adapter);

    }

    @SuppressWarnings("unchecked")
    @GET
    public DocumentModelList getDocs(@QueryParam("page")
    @DefaultValue("0")
    Long page, @QueryParam("pagesize")
    Long pageSize) throws ClientException {

        DocumentObject dobj = (DocumentObject) getTarget();

        PageProviderService ppService = Framework.getLocalService(PageProviderService.class);
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY,
                (Serializable) ctx.getCoreSession());
        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) ppService.getPageProvider(
                "CURRENT_DOC_CHILDREN", null, pageSize, null, props,
                new Object[] { dobj.getDocument().getId() });
        pp.setCurrentPage(page);
        return new DocumentModelListImpl(pp.getCurrentPage());
    }
}
