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
 *     vpasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.server.jaxrs.adapters;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLBlob;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.web.common.ServletHelper;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;

/**
 * @since 5.7.3 - REST API Blob Manager
 */
@WebAdapter(name = BlobAdapter.NAME, type = "blob")
public class BlobAdapter extends DefaultAdapter {

    public final static String NAME = "blob";

    protected String xpath;

    protected DocumentModel doc;

    @Override
    protected void initialize(Object... args) {
        super.initialize(args);
        if (args.length == 2) {
            xpath = (String) args[0];
            doc = (DocumentModel) args[1];
        }
    }

    @Path("{xpath}")
    public Resource doGet(@PathParam("xpath")
    String xpath) {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        return newObject("blob", xpath, doc);
    }

    @Override
    public <A> A getAdapter(Class<A> adapter) {
        if (adapter.isAssignableFrom(Blob.class)) {
            try {
                return adapter.cast(blob(xpath));
            } catch (ClientException e) {
                throw WebException.wrap("Could not find any uploaded blob: "
                        + xpath, e);
            }
        }
        return super.getAdapter(adapter);
    }

    protected Blob blob(String xpath) throws ClientException {
        return (Blob) doc.getPropertyValue(xpath);
    }

    @GET
    public Response doGet(@Context
    Request request) throws ClientException {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        FormData form = ctx.getForm();
        String xpath = form.getString(FormData.PROPERTY);
        // TODO parse all blob properties to fetch all related xpath
        if (doc.hasSchema("file")) {
            xpath = "file:content";
        }
        try {
            Property p = doc.getProperty(xpath);
            Blob blob = (Blob) p.getValue();
            if (blob == null) {
                throw new WebResourceNotFoundException("No attached file at "
                        + xpath);
            }
            String fileName = blob.getFilename();
            if (fileName == null) {
                p = p.getParent();
                if (p.isComplex()) { // special handling for file and files
                    // schema
                    try {
                        fileName = (String) p.getValue("filename");
                    } catch (PropertyException e) {
                        fileName = "Unknown";
                    }
                }
            }
            EntityTag etag = null;
            if (blob instanceof SQLBlob) {
                etag = new EntityTag(((SQLBlob) blob).getBinary().getDigest());
            }
            if (etag != null) {
                Response.ResponseBuilder builder = request.evaluatePreconditions(etag);
                if (builder != null) {
                    return builder.build();
                }
            }
            String contentDisposition = ServletHelper.getRFC2231ContentDisposition(
                    ctx.getRequest(), fileName);
            // cached resource did change or no ETag -> serve updated content
            Response.ResponseBuilder builder = Response.ok(blob).header(
                    "Content-Disposition", contentDisposition).type(
                    blob.getMimeType());
            if (etag != null) {
                builder.tag(etag);
            }
            return builder.build();
        } catch (Exception e) {
            throw WebException.wrap("Failed to get the attached file", e);
        }
    }

    @POST
    public Response doPost() {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        FormData form = ctx.getForm();
        form.fillDocument(doc);
        Blob blob = form.getFirstBlob();
        if (blob == null) {
            throw new IllegalArgumentException(
                    "Could not find any uploaded file");
        }
        try {
            Property p = doc.getProperty(xpath);
            if (p.isList()) { // add the file to the list
                if ("files".equals(p.getSchema().getName())) {
                    Map<String, Serializable> map = new HashMap<String, Serializable>();
                    map.put("filename", blob.getFilename());
                    map.put("file", (Serializable) blob);
                    p.addValue(map);
                } else {
                    p.addValue(blob);
                }
            } else {
                if ("file".equals(p.getSchema().getName())) {
                    p.getParent().get("filename").setValue(blob.getFilename());
                }
                p.setValue(blob);
            }
            // make snapshot
            doc.putContextData(VersioningService.VERSIONING_OPTION,
                    form.getVersioningOption());
            CoreSession session = ctx.getCoreSession();
            session.saveDocument(doc);
            session.save();
            return redirect(getTarget().getPath());
        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw WebException.wrap("Failed to attach file", e);
        }
    }

    @DELETE
    public Response doDelete() {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        try {
            doc.getProperty(xpath).remove();
            CoreSession session = ctx.getCoreSession();
            session.saveDocument(doc);
            session.save();
        } catch (PropertyException e) {
            throw WebException.wrap(
                    "Failed to delete attached file into property: " + xpath, e);
        } catch (ClientException e) {
            throw WebException.wrap(
                    "Failed to delete attached file into property: " + xpath, e);
        }
        return redirect(getTarget().getPath());
    }

}
