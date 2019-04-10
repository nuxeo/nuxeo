/*
 * (C) Copyright 2014-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     annejubert
 */
package org.nuxeo.io.fsexporter;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.core.CoreQueryPageProviderDescriptor;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.runtime.api.Framework;

public class DefaultExporterPlugin implements FSExporterPlugin {

    @Override
    public DocumentModelList getChildren(CoreSession session, DocumentModel doc, String customQuery) {
        Map<String, Serializable> props = new HashMap<>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) session);

        String query = "";
        // if the user gives a query, we build a new Page Provider with the query provided
        if (StringUtils.isNotBlank(customQuery)) {
            if (customQuery.toLowerCase().contains(" where")) {
                query = customQuery + " AND ecm:parentId = ?";
            } else {
                query = customQuery + " where ecm:parentId = ?";
            }
        } else {
            query = "SELECT * FROM Document WHERE ecm:parentId = ? AND ecm:mixinType !='HiddenInNavigation' AND ecm:isCheckedInVersion = 0 AND ecm:currentLifeCycleState !='deleted'";
        }
        CoreQueryPageProviderDescriptor desc = new CoreQueryPageProviderDescriptor();
        desc.setPattern(query);

        PageProviderService ppService = Framework.getService(PageProviderService.class);
        @SuppressWarnings("unchecked")
        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) ppService.getPageProvider("customPP", desc,
                null, null, null, null, props, new Object[] { doc.getId() });

        int countPages = 1;
        // get all the documents of the first page
        DocumentModelList children = new DocumentModelListImpl(pp.getCurrentPage());
        // if there is more than one page, get the children of all the other pages and put into one list
        if (pp.getNumberOfPages() > 1) {
            while (countPages < pp.getNumberOfPages()) {
                pp.nextPage();
                List<DocumentModel> childrenTemp = pp.getCurrentPage();
                for (DocumentModel childTemp : childrenTemp) {
                    children.add(childTemp);
                }
                countPages++;
            }
        }
        // return the complete list of documents
        return children;
    }

    @Override
    public File serialize(CoreSession session, DocumentModel docfrom, String fsPath) throws IOException {
        File folder = null;
        File newFolder = null;
        folder = new File(fsPath);

        // if target directory doesn't exist, create it
        if (!folder.exists()) {
            folder.mkdir();
        }

        if ("/".equals(docfrom.getPathAsString())) {
            // we do not serialize the root document
            return folder;
        }

        if (docfrom.hasFacet("Folderish")) {
            newFolder = new File(fsPath + "/" + docfrom.getName());
            newFolder.mkdir();
        }

        // get all the blobs of the blob holder
        BlobHolder myblobholder = docfrom.getAdapter(BlobHolder.class);
        if (myblobholder != null) {
            java.util.List<Blob> listblobs = myblobholder.getBlobs();
            int i = 1;
            for (Blob blob : listblobs) {
                // call the method to determine the name of the exported file
                String FileNameToExport = getFileName(blob, docfrom, folder, i);
                // export the file to the target file system
                File target = new File(folder, FileNameToExport);
                blob.transferTo(target);
                i++;
            }
        }
        if (newFolder != null) {
            folder = newFolder;
        }
        return folder;
    }

    protected String getFileName(Blob blob, DocumentModel docfrom, File folder, int i) {
        String prefix = "";
        // if not principal file, prefix = name of the file which contains the blobs
        if (i != 1) {
            prefix = docfrom.getName() + "-";
        }

        // if already existing file, prefix with "timestamp"
        File alreadyExistingBlob = new File(folder, prefix + blob.getFilename());

        if (alreadyExistingBlob.exists()) {
            prefix = String.valueOf(new Date().getTime()) + "-" + prefix;
        }

        return prefix + blob.getFilename();
    }
}
