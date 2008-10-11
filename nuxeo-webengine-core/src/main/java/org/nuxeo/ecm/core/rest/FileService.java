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
 *
 * $Id$
 */

package org.nuxeo.ecm.core.rest;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.NoSuchResourceException;
import org.nuxeo.ecm.webengine.model.WebService;
import org.nuxeo.ecm.webengine.model.impl.DefaultService;

/**
 * File Service - manage attachments on a document

 * <p>
 * Accepts the following methods:
 * <ul>
 * <li> GET - get the attached file
 * <li> POST - create an attachment
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@WebService(name="file", targetTypes={"Document"})
public class FileService extends DefaultService {

    @GET
    public Object doGet() {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        FormData form = ctx.getForm();
        String xpath = form.getString(FormData.PROPERTY);
        if (xpath == null) {
            if (doc.hasSchema("file")) {
                xpath = "file:content";
            } else {
                throw new IllegalArgumentException(
                "Missing request parameter named 'property' that specify the blob property xpath to fetch");
            }
        }
        try {
            Property p = doc.getProperty(xpath);
            Blob blob = (Blob)p.getValue();
            if (blob == null) {
                throw new NoSuchResourceException("No attached file at "+xpath);
            }
            String fileName = blob.getFilename();
            if (fileName == null) {
                p = p.getParent();
                if (p.isComplex()) { // special handling for file and files schema
                    try {
                        fileName = (String)p.getValue("filename");
                    } catch (PropertyException e) {
                        fileName = "Unknown";
                    }
                }
            }
            return Response.ok()
                .header("Content-Disposition","inline; filename="+fileName)
                .type(blob.getMimeType())
                .entity(blob).build();
        } catch (Exception e) {
            throw new WebException("Failed to get the attached file", e);
        }
    }

    @POST
    public Object doPost() {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        FormData form = ctx.getForm();
        String xpath = ctx.getForm().getString(FormData.PROPERTY);
        if (xpath == null) {
            if (doc.hasSchema("file")) {
                xpath = "file:content";
            } else if (doc.hasSchema("files")) {
                xpath = "files:files";
            } else {
                throw new IllegalArgumentException(
                        "Missing request parameter named 'property' that specifies the blob property xpath to fetch");
            }
        }
        Blob blob = form.getFirstBlob();
        if (blob == null) {
            throw new IllegalArgumentException("Could not find any uploaded file");
        }
        try {
            Property p = doc.getProperty(xpath);
            if (p.isList()) { // add the file to the list
                if (p.getSchema().getName().equals("files")) { // treat the files schema separately
                    Map<String, Serializable> map = new HashMap<String, Serializable>();
                    map.put("filename", blob.getFilename());
                    map.put("file", (Serializable)blob);
                    p.add(map);
                } else {
                    p.add(blob);
                }
            } else {
                if (p.getSchema().getName().equals("file")) { // for compatibility with deprecated filename
                    p.getParent().get("filename").setValue(blob.getFilename());
                }
                p.setValue(blob);
            }
            CoreSession session = ctx.getCoreSession();
            session.saveDocument(doc);
            session.save();
            return null; //TODO
        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw new WebException("Failed to attach file", e);
        }
    }


}
