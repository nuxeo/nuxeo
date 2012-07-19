/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.documentation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.apidoc.api.AbstractDocumentationItem;
import org.nuxeo.apidoc.api.AssociatedDocuments;
import org.nuxeo.apidoc.api.DocumentationItem;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;

public class AssociatedDocumentsImpl implements AssociatedDocuments {

    protected String id;

    protected final NuxeoArtifact item;

    protected final CoreSession session;

    protected Map<String, ResourceDocumentationItem> liveDoc;

    protected final Log log = LogFactory.getLog(AssociatedDocumentsImpl.class);

    public AssociatedDocumentsImpl(NuxeoArtifact item, CoreSession session) {
        this.item = item;
        this.session = session;
    }

    @Override
    public Map<String, List<DocumentationItem>> getDocumentationItems(
            CoreSession session) throws Exception {
        DocumentationService ds = Framework.getLocalService(DocumentationService.class);
        List<DocumentationItem> docItems = ds.findDocumentItems(session, item);
        Map<String, String> categories = getCategories();
        Map<String, List<DocumentationItem>> result = new LinkedHashMap<String, List<DocumentationItem>>();
        // put categories in result in same order
        List<String> empty = new ArrayList<String>();
        for (String catLabel : categories.values()) {
            result.put(catLabel, new ArrayList<DocumentationItem>());
            empty.add(catLabel);
        }
        // read items
        for (DocumentationItem docItem : docItems) {
            String cat = docItem.getType();
            String catLabel = categories.get(cat);
            result.get(catLabel).add(docItem);
            empty.remove(catLabel);
        }
        // add virtual items
        if (liveDoc != null) {
            for (String vCat : liveDoc.keySet()) {
                String catLabel = categories.get(vCat);
                if (result.get(catLabel) != null) {
                    result.get(catLabel).add(liveDoc.get(vCat));
                    empty.remove(catLabel);
                } else {
                    log.warn("Live doc for category " + catLabel
                            + " won't be visible");
                }
            }
        }
        // clear empty
        for (String catLabel : empty) {
            result.remove(catLabel);
        }
        return result;
    }

    @Override
    public Map<String, String> getCategories() throws Exception {
        DocumentationService ds = Framework.getLocalService(DocumentationService.class);
        return ds.getCategories();
    }

    @Override
    public List<String> getCategoryKeys() throws Exception {
        DocumentationService ds = Framework.getLocalService(DocumentationService.class);
        return ds.getCategoryKeys();
    }

    @Override
    public DocumentationItem getDescription(CoreSession session)
            throws Exception {
        DocumentationService ds = Framework.getLocalService(DocumentationService.class);
        List<DocumentationItem> docItems = ds.findDocumentItems(session, item);
        for (DocumentationItem docItem : docItems) {
            String cat = docItem.getType();
            if (DefaultDocumentationType.DESCRIPTION.getValue().equals(cat)) {
                return docItem;
            }
        }

        return new AbstractDocumentationItem() {

            @Override
            public boolean isApproved() {
                return false;
            }

            @Override
            public String getUUID() {
                return null;
            }

            @Override
            public String getTypeLabel() {
                return null;
            }

            @Override
            public String getType() {
                return null;
            }

            @Override
            public String getTitle() {
                if (item.getArtifactType().equals(ExtensionPointInfo.TYPE_NAME)) {
                    return ((ExtensionPointInfo) item).getName();
                } else if (item.getArtifactType().equals(
                        ExtensionInfo.TYPE_NAME)) {
                    return ((ExtensionInfo) item).getExtensionPoint();
                } else if (item.getArtifactType().equals(ServiceInfo.TYPE_NAME)) {
                    String id = item.getId();
                    String[] parts = id.split("\\.");
                    if (parts.length > 1) {
                        String name = parts[parts.length - 1];
                        return name;
                    }
                }
                return item.getId();
            }

            @Override
            public String getTargetType() {
                return item.getArtifactType();
            }

            @Override
            public String getTarget() {
                return item.getId();
            }

            @Override
            public String getRenderingType() {
                return "html";
            }

            @Override
            public String getId() {
                return null;
            }

            @Override
            public String getContent() {
                String content = liveDoc.get(
                        DefaultDocumentationType.DESCRIPTION.getValue()).getContent();
                if (content == null) {
                    content = "";
                }
                return content;
            }

            @Override
            public List<String> getApplicableVersion() {
                return null;
            }

            @Override
            public Map<String, String> getAttachments() {
                return new HashMap<String, String>();
            }

            @Override
            public boolean isPlaceHolder() {
                return true;
            }

            @Override
            public String getEditId() {
                return "placeholder_" + item.getId();
            }
        };

    }

    public void setLiveDoc(Map<String, ResourceDocumentationItem> liveDoc) {
        this.liveDoc = liveDoc;
    }

}
