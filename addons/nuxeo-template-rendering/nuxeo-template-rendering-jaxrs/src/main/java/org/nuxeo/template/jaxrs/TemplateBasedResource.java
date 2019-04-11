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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

@WebObject(type = "templateBasedResource")
public class TemplateBasedResource extends DefaultObject {

    protected String uuid;

    public TemplateBasedResource() {
        uuid = null;
    }

    public TemplateBasedResource(String uuid) {
        this.uuid = uuid;
    }

    @Override
    protected void initialize(Object... args) {
        if (args != null && args.length > 0) {
            this.uuid = (String) args[0];
        }
    }

    protected TemplateBasedDocument resolve() {
        if (uuid != null) {
            IdRef idRef = new IdRef(uuid);
            DocumentModel doc = getContext().getCoreSession().getDocument(idRef);
            return doc.getAdapter(TemplateBasedDocument.class);
        } else {
            return null;
        }
    }

    @GET
    public Object get() {
        TemplateBasedDocument tmpl = resolve();
        if (tmpl == null) {
            return "";
        } else {
            return tmpl.getAdaptedDoc().getId() + " - " + tmpl.getAdaptedDoc().getTitle();
        }
    }

    @GET
    @Path("templates")
    public String getAssociatedTemplates() {

        IdRef idRef = new IdRef(uuid);
        DocumentModel doc = getContext().getCoreSession().getDocument(idRef);
        TemplateBasedDocument tmpl = doc.getAdapter(TemplateBasedDocument.class);
        if (tmpl == null) {
            return "This document is not template based";
        }
        return tmpl.getTemplateNames().toString();
    }

    @Path("template/{name}")
    public Object getAssociatedTemplate(@PathParam(value = "name") String name) {

        IdRef idRef = new IdRef(uuid);
        DocumentModel doc = getContext().getCoreSession().getDocument(idRef);
        TemplateBasedDocument tmpl = doc.getAdapter(TemplateBasedDocument.class);
        if (tmpl == null) {
            return "This document is not template based";
        }

        DocumentRef sourceRef = tmpl.getSourceTemplateDocRef(name);
        if (sourceRef != null) {
            return getContext().newObject("templateResource", sourceRef.toString());
        }
        return null;
    }

    @GET
    @Path("resource/{templateName}/{resourceName}")
    @Produces("*/*")
    public Blob getResource(@PathParam(value = "templateName") String templateName,
            @PathParam(value = "resourceName") String resourceName) {

        TemplateBasedDocument tmpl = resolve();

        BlobHolder bh = tmpl.getAdaptedDoc().getAdapter(BlobHolder.class);
        if (bh != null) {
            for (Blob blob : bh.getBlobs()) {
                if (resourceName.equalsIgnoreCase(blob.getFilename())) {
                    return blob;
                }
            }
        }

        TemplateSourceDocument template = tmpl.getSourceTemplate(templateName);
        if (template != null) {
            return TemplateResource.getResource(template, resourceName);
        }
        return null;
    }
}
