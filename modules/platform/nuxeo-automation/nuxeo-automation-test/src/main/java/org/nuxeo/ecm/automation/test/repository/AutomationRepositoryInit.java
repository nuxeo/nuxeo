/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.test.repository;

import java.io.File;
import java.io.Serializable;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 7.3
 */
public class AutomationRepositoryInit extends DefaultRepositoryInit {

    @Override
    public void populate(CoreSession session) {
        super.populate(session);
        DocumentModel doc = session.createDocumentModel("/", "testBlob", "File");
        doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob("one"));
        session.createDocument(doc);
        File docFile = FileUtils.getResourceFileFromContext("hello.doc");
        DocumentModel doc2 = session.createDocumentModel("/", "testBlob2", "File");
        doc2.setPropertyValue("file:content", new FileBlob(docFile));
        session.createDocument(doc2);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
    }
}
