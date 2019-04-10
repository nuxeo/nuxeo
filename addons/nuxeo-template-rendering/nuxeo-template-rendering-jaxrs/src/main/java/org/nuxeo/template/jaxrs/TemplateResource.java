/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.template.jaxrs;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.TemplateProcessorService;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

@WebObject(type = "templateResource")
public class TemplateResource extends DefaultObject {

    protected String uuid;

    public TemplateResource() {
        uuid = null;
    }

    protected void initialize(Object... args) {
        if (args != null && args.length > 0) {
            this.uuid = (String) args[0];
        }
    }

    public TemplateResource(String value) {
        this.uuid = value;
    }

    protected TemplateSourceDocument resolve() {
        if (uuid != null) {
            IdRef idRef = new IdRef(uuid);
            DocumentModel doc = getContext().getCoreSession().getDocument(idRef);
            return doc.getAdapter(TemplateSourceDocument.class);
        } else {
            return null;
        }
    }

    @GET
    public Object get() {
        TemplateSourceDocument source = resolve();
        if (source == null) {
            return getList();
        } else {
            StringBuffer sb = new StringBuffer();
            sb.append(source.getId() + " - " + source.getLabel());
            return sb.toString();
        }
    }

    protected String getList() {

        TemplateProcessorService tps = Framework.getService(TemplateProcessorService.class);
        List<TemplateSourceDocument> sources = tps.getAvailableTemplates(getContext().getCoreSession(), null);

        StringBuffer sb = new StringBuffer();
        for (TemplateSourceDocument source : sources) {
            sb.append(source.getId() + " - " + source.getName() + "-" + source.getLabel());
            sb.append("\n");
        }
        return sb.toString();
    }

    @GET
    @Path("resource/{resourceName}")
    @Produces("*/*")
    public Blob getResource(@PathParam(value = "resourceName") String resourceName) {
        TemplateSourceDocument tmpl = resolve();
        return getResource(tmpl, resourceName);
    }

    static Blob getResource(TemplateSourceDocument tmpl, String resourceName) {

        BlobHolder bh = tmpl.getAdaptedDoc().getAdapter(BlobHolder.class);
        if (bh != null) {
            for (Blob blob : bh.getBlobs()) {
                if (resourceName.equalsIgnoreCase(blob.getFilename())) {
                    return blob;
                }
            }
        }
        return null;
    }

}
