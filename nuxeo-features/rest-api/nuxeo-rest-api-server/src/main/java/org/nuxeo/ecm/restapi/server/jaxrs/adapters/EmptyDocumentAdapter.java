/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.restapi.server.jaxrs.adapters;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.exceptions.IllegalParameterException;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;

/**
 * @since 9.3
 */
@WebAdapter(name = EmptyDocumentAdapter.NAME, type = "emptyDocumentAdapter")
@Produces(MediaType.APPLICATION_JSON)
public class EmptyDocumentAdapter extends DefaultAdapter {

    public static final String NAME = "emptyWithDefault";

    @GET
    public DocumentModel getEmptyDocumentModel(@QueryParam("type") String type, @QueryParam("name") String name) {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        CoreSession session = ctx.getCoreSession();

        if (StringUtils.isBlank(type)) {
            throw new IllegalParameterException("Missing type parameter");
        }

        DocumentModel emptyDoc = session.createDocumentModel(doc != null ? doc.getPathAsString() : null, name, type);
        emptyDoc.detach(false);
        return emptyDoc;
    }
}
