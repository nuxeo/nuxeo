/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.apidoc.documentation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.api.AssociatedDocuments;
import org.nuxeo.apidoc.api.DocumentationItem;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;

public class AssociatedDocumensImpl implements AssociatedDocuments {

    protected String id;

    protected NuxeoArtifact item;

    protected CoreSession session;

    public AssociatedDocumensImpl(NuxeoArtifact item, CoreSession session) {
        this.item = item;
        this.session = session;
    }

    public Map<String, List<DocumentationItem>> getDocumentationItems(CoreSession session) throws Exception {

        DocumentationService ds = Framework.getLocalService(DocumentationService.class);

        List<DocumentationItem> docItems = ds.findDocumentItems(session, item);

        Map<String, List<DocumentationItem>> result = new HashMap<String, List<DocumentationItem>>();

        Map<String, String> categories = getCategories();

        for (DocumentationItem docItem : docItems) {

            String cat = docItem.getType();
            String catLabel = categories.get(cat);

            List<DocumentationItem> itemList = result.get(catLabel);

            if (itemList!=null) {
                itemList.add(docItem);
            } else {
                itemList = new ArrayList<DocumentationItem>();
                itemList.add(docItem);
                result.put(catLabel, itemList);
            }
        }
        return result;
    }



    public Map<String, String> getCategories() throws Exception {
        DocumentationService ds = Framework.getLocalService(DocumentationService.class);
        return ds.getCategories();
    }


    public List<String> getCategoryKeys() throws Exception {
        DocumentationService ds = Framework.getLocalService(DocumentationService.class);
        return ds.getCategoryKeys();
    }



    public DocumentationItem getDescription(CoreSession session) throws Exception {
        DocumentationService ds = Framework.getLocalService(DocumentationService.class);
        List<DocumentationItem> docItems = ds.findDocumentItems(session, item);
        for (DocumentationItem docItem : docItems) {
            String cat = docItem.getType();
            if ("description".equals(cat)) {
                return docItem;
            }
        }

        return new DocumentationItem() {

            public boolean isApproved() {
                return false;
            }

            public String getUUID() {
                return null;
            }

            public String getTypeLabel() {
                return null;
            }

            public String getType() {
                return null;
            }

            public String getTitle() {
                if (item.getArtifactType().equals(ExtensionPointInfo.TYPE_NAME)) {
                    return ((ExtensionPointInfo)item).getName();
                } else if (item.getArtifactType().equals(ExtensionInfo.TYPE_NAME)) {
                    return ((ExtensionInfo)item).getExtensionPoint();
                } else if (item.getArtifactType().equals(ServiceInfo.TYPE_NAME)) {
                    String id = ((ServiceInfo)item).getId();
                    String[] parts = id.split("\\.");
                    if (parts.length>1) {
                        String name = parts[parts.length-1];
                        return name;
                    }
                }
                return item.getId();
            }

            public String getTargetType() {
                return item.getArtifactType();
            }

            public String getTarget() {
                return item.getId();
            }

            public String getRenderingType() {
                return "html";
            }

            public String getId() {
                return null;
            }

            public String getContent() {
                return "";
            }

            public List<String> getApplicableVersion() {
                return null;
            }

            public Map<String, String> getAttachements() {
                return new HashMap<String, String>();
            }
        };


    }

}
