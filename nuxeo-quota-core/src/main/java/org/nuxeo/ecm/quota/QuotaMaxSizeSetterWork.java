/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Mariana Cedica <mcedica@nuxeo.com>
 */

package org.nuxeo.ecm.quota;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.quota.size.QuotaAware;
import org.nuxeo.ecm.quota.size.QuotaAwareDocumentFactory;

/**
 * Work to set the maxSize on a list of documents
 *
 * @since 5.7
 *
 */
public class QuotaMaxSizeSetterWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    public long maxSize;

    public static final String QUOTA_MAX_SIZE_UPDATE_WORK = "quotaMaxSizeSetter";

    public QuotaMaxSizeSetterWork(long maxSize, List<String> docIds,
            String repositoryName) {
        super(); // random id, for unique job
        setDocuments(repositoryName, docIds);
        this.maxSize = maxSize;
    }

    @Override
    public String getTitle() {
        return QUOTA_MAX_SIZE_UPDATE_WORK;
    }

    @Override
    public String getCategory() {
        return QUOTA_MAX_SIZE_UPDATE_WORK;
    }

    public void notifyProgress(long current) {
        setProgress(new Progress(current, docIds.size()));
    }

    @Override
    public void work() {
        new UnrestrictedSessionRunner(repositoryName) {

            @Override
            public void run() throws ClientException {
                for (String docId : docIds) {
                    DocumentModel doc = session.getDocument(new IdRef(docId));
                    QuotaAware qa = doc.getAdapter(QuotaAware.class);
                    if (qa == null) {
                        qa = QuotaAwareDocumentFactory.make(doc, true);
                    }
                    // skip validation on other children quotas
                    qa.setMaxQuota(maxSize, true, true);
                }
            }
        }.runUnrestricted();
    }
}
