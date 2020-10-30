/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Mariana Cedica <mcedica@nuxeo.com>
 */

package org.nuxeo.ecm.quota;

import java.util.List;

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
 */
public class QuotaMaxSizeSetterWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    public long maxSize;

    public static final String QUOTA_MAX_SIZE_UPDATE_WORK = "quotaMaxSizeSetter";

    public QuotaMaxSizeSetterWork(long maxSize, List<String> docIds, String repositoryName) {
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
            public void run() {
                for (String docId : docIds) {
                    DocumentModel doc = session.getDocument(new IdRef(docId));
                    QuotaAware qa = QuotaAwareDocumentFactory.make(doc);
                    // skip validation on other children quotas
                    qa.setMaxQuota(maxSize, true);
                    qa.save();
                }
            }
        }.runUnrestricted();
    }
}
