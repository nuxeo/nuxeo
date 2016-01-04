/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */
package org.nuxeo.ecm.liveconnect.update.worker;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.liveconnect.update.BatchUpdateBlobProvider;
import org.nuxeo.runtime.api.Framework;

public class BlobProviderDocumentsUpdateWork extends AbstractWork {

    private static final Log log = LogFactory.getLog(BlobProviderDocumentsUpdateWork.class);

    private static final long serialVersionUID = 1L;

    protected static final String TITLE = "Live Connect Update Documents Work";

    public static final String CATEGORY = "blobProviderDocumentsUpdate";

    protected String providerName;

    public BlobProviderDocumentsUpdateWork(final String id, final String providerName) {
        super(id);
        this.providerName = providerName;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public void work() {
        BatchUpdateBlobProvider blobProvider = (BatchUpdateBlobProvider) Framework.getService(
                BlobManager.class).getBlobProvider(providerName);
        setStatus("Updating");
        if (session == null) {
            initSession();
        }
        final List<DocumentModel> results = docIds.stream().map(IdRef::new).map(session::getDocument)
                .collect(Collectors.toList());
        log.trace("Updating");
        List<DocumentModel> changedDocuments = blobProvider.checkChangesAndUpdateBlob(results);
        if (changedDocuments != null) {
            for (DocumentModel doc : changedDocuments) {
                session.saveDocument(doc);
            }
        }
        log.trace("Updating done");
        setStatus("Done");
    }

}
