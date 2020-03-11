/*
 * Copyright (c) 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.io.impl.extensions;

import java.util.Calendar;

import org.dom4j.Element;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.lock.LockManager;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.ImportExtension;
import org.nuxeo.ecm.core.storage.lock.LockManagerService;
import org.nuxeo.runtime.api.Framework;

/**
 * Allows to import Document Locks
 *
 * @since 7.4
 */
// TODO Is it really used ?
public class DocumentLockImporter implements ImportExtension {

    @Override
    public void updateImport(CoreSession session, DocumentModel docModel, ExportedDocument xdoc) throws Exception {

        Element lockInfo = xdoc.getDocument().getRootElement().element("lockInfo");
        if (lockInfo != null) {

            String createdMS = lockInfo.element("created").getText();
            String owner = lockInfo.element("owner").getText();

            Calendar created = Calendar.getInstance();
            created.setTimeInMillis(Long.parseLong(createdMS));
            Lock lock = new Lock(owner, created);

            getLockManager(session).setLock(docModel.getId(), lock);
        }

    }

    protected LockManager getLockManager(CoreSession session) {
        LockManagerService lms = Framework.getService(LockManagerService.class);
        return lms.getLockManager(session.getRepositoryName());
    }
}