/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.restapi.server.jaxrs.rendition;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 7.2
 */
@WebAdapter(name = RenditionAdapter.NAME, type = "renditionAdapter")
public class RenditionAdapter extends DefaultAdapter {

    public static final String NAME = "rendition";

    @GET
    @Path("{renditionName}")
    public Object doGetRendition(@Context Request request, @PathParam("renditionName") String renditionName) {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        RenditionService renditionService = Framework.getService(RenditionService.class);

        Rendition rendition = renditionService.getRendition(doc, renditionName);
        if (rendition == null) {
            throw new WebResourceNotFoundException(String.format("No rendition '%s' was found", renditionName));
        }

        Blob blob = rendition.getBlob();
        if (blob == null) {
            throw new WebResourceNotFoundException(String.format("No Blob was found for rendition '%s'", renditionName));
        }

        return blob;
    }
}
