/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.automation.core.util;

import org.codehaus.jackson.node.ObjectNode;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;

/**
 * Very basic implementation of a Blob decoder Only usable for StringBlobs
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.5
 */
public class JSONStringBlobDecoder implements JSONBlobDecoder {
    public Blob getBlobFromJSON(ObjectNode jsonObject) {
        Blob blob = null;

        String filename = null;
        if (jsonObject.has("filename")) {
            filename = jsonObject.get("filename").getTextValue();
        }
        if (filename == null && jsonObject.has("name")) {
            filename = jsonObject.get("name").getTextValue();
        }
        String encoding = "UTF-8";
        if (jsonObject.has("encoding")) {
            encoding = jsonObject.get("encoding").getTextValue();
        }

        String mimetype = "text/plain";
        if (jsonObject.has("mime-type")) {
            mimetype = jsonObject.get("mime-type").getTextValue();
        }
        String data = null;
        if (jsonObject.has("data")) {
            data = jsonObject.get("data").getTextValue();
        } else if (jsonObject.has("content")) {
            data = jsonObject.get("content").getTextValue();
        }
        if (data == null) {
            return null;
        } else {
            blob = new StringBlob(data, mimetype, encoding);
        }
        return blob;
    }
}
