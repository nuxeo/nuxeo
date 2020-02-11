/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.apidoc.documentation;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.LinkedMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.apidoc.api.AbstractDocumentationItem;
import org.nuxeo.apidoc.api.DocumentationItem;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;

public class DocumentationItemDocAdapter extends AbstractDocumentationItem implements DocumentationItem {

    protected static final Log log = LogFactory.getLog(DocumentationItemDocAdapter.class);

    protected final DocumentModel doc;

    public DocumentationItemDocAdapter(DocumentModel doc) {
        super(typeLabelOf(doc.getProperty(PROP_TYPE).getValue(String.class)));
        this.doc = doc;
    }

    public DocumentModel getDocumentModel() {
        return doc;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getApplicableVersion() {
        try {
            return (List<String>) doc.getPropertyValue(PROP_APPLICABLE_VERSIONS);
        } catch (PropertyException e) {
            log.error("Error while reading applicable version", e);
            return new ArrayList<>();
        }
    }

    @Override
    public String getContent() {
        String encoding = "unset";
        try {
            Blob blob = (Blob) doc.getPropertyValue("file:content");
            if (blob == null) {
                return "";
            }
            encoding = blob.getEncoding();
            if (encoding == null || encoding.equals("")) {
                blob.setEncoding("utf-8");
            }
            return blob.getString();
        } catch (IOException | PropertyException e) {
            log.error("Error while reading content with encoding " + encoding, e);
            return "ERROR : " + e.getMessage();
        }
    }

    @Override
    public String getRenderingType() {
        try {
            return (String) doc.getPropertyValue(PROP_RENDERING_TYPE);
        } catch (PropertyException e) {
            log.error("Error while reading rendering type", e);
            return "";
        }
    }

    @Override
    public String getTarget() {
        try {
            return (String) doc.getPropertyValue(PROP_TARGET);
        } catch (PropertyException e) {
            log.error("Error while reading target", e);
            return "";
        }
    }

    @Override
    public String getTargetType() {
        try {
            return (String) doc.getPropertyValue(PROP_TARGET_TYPE);
        } catch (PropertyException e) {
            log.error("Error while reading targetType", e);
            return "";
        }
    }

    @Override
    public String getType() {
        try {
            return (String) doc.getPropertyValue(PROP_TYPE);
        } catch (PropertyException e) {
            log.error("Error while reading type", e);
            return "";
        }
    }

    @Override
    public boolean isApproved() {
        try {
            Boolean approved = (Boolean) doc.getPropertyValue(PROP_NUXEO_APPROVED);
            return approved == null ? false : approved.booleanValue();
        } catch (PropertyException e) {
            log.error("Error while reading type", e);
            return false;
        }
    }

    @Override
    public String getId() {
        try {
            return (String) doc.getPropertyValue(PROP_DOCUMENTATION_ID);
        } catch (PropertyException e) {
            log.error("Error while reading target", e);
            return "";
        }
    }

    @Override
    public String getUUID() {
        return doc.getId();
    }

    @Override
    public String getTitle() {
        try {
            return (String) doc.getPropertyValue("dc:title");
        } catch (PropertyException e) {
            log.error("Error while reading title", e);
            return "";
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> getAttachments() {
        Map<String, String> attachments = new LinkedMap();
        try {
            List<Map<String, Serializable>> atts = (List<Map<String, Serializable>>) doc.getPropertyValue(
                    "files:files");
            if (atts != null) {
                for (Map<String, Serializable> att : atts) {
                    Blob attBlob = (Blob) att.get("file");
                    if (attBlob.getEncoding() == null || attBlob.getEncoding().equals("")) {
                        attBlob.setEncoding("utf-8");
                    }
                    attachments.put((String) att.get("filename"), attBlob.getString());
                }
            }
        } catch (IOException | PropertyException e) {
            log.error("Error while reading Attachments", e);
        }
        return attachments;
    }

    @Override
    public boolean isPlaceHolder() {
        return false;
    }

    @Override
    public String getEditId() {
        return getUUID();
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }
}
