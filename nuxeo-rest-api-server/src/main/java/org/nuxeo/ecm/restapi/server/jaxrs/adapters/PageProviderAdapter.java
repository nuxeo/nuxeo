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
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.nuxeo.ecm.automation.core.util.Paginable;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.runtime.api.Framework;

/**
 * Adapter that expose a page provider that needs only one parameter that is the
 * document Id
 *
 * @since 5.7.2
 */
@WebAdapter(name = PageProviderAdapter.NAME, type = "PageProviderService")
@Produces({ "application/json+nxentity", "application/json+esentity", MediaType.APPLICATION_JSON })
public class PageProviderAdapter extends DocumentModelListPaginableAdapter {

    public static final String NAME = "pp";

    private String pageProviderName = "CURRENT_DOC_CHILDREN";

    @Override
    protected PageProviderDefinition getPageProviderDefinition() {
        PageProviderService ppService = Framework.getLocalService(PageProviderService.class);
        return ppService.getPageProviderDefinition(pageProviderName);
    }

    @Override
    protected Object[] getParams() {
        return new Object[] { getTarget().getAdapter(DocumentModel.class).getId() };
    }

    @GET
    @Path("{pageProviderName}")
    public Paginable<DocumentModel> getProviderDocs(
            @PathParam("pageProviderName")
            String providerName) throws ClientException {
        pageProviderName = providerName;
        return super.getPaginableEntries();
    }

}
