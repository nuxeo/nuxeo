/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.test.repository;

import java.io.File;
import java.io.Serializable;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.ClientException;
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
    public void populate(CoreSession session) throws ClientException {
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
