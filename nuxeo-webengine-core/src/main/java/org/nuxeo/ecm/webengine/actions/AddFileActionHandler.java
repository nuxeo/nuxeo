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

package org.nuxeo.ecm.webengine.actions;


import java.io.Serializable;
import java.util.HashMap;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.WebObject;
import org.nuxeo.ecm.webengine.WebContext;
import org.nuxeo.ecm.webengine.util.FormData;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class AddFileActionHandler implements ActionHandler {

    public void run(WebObject object) throws WebException {
        if (!object.isResolved()) {
            throw new WebException("Cannot run getFile action on a non resolved object: "+object);
        }
        DocumentModel doc = object.getDocument();
        WebContext req = object.getSiteRequest();
        FormData form = req.getForm();
        String xpath = form.getString(FormData.PROPERTY);
        if (xpath == null) {
            if (doc.hasSchema("file")) {
                xpath = "file:content";
            } else if (doc.hasSchema("files")) {
                xpath = "files:files";
            } else {
                throw new IllegalArgumentException("Missing request parameter named 'property' that specify the blob property xpath to fetch");
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
                    HashMap<String, Serializable> map = new HashMap<String, Serializable>();
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
            object.getSession().saveDocument(doc);
        } catch (Exception e) {
            throw new WebException("Failed to attach file", e);
        }
    }

}
