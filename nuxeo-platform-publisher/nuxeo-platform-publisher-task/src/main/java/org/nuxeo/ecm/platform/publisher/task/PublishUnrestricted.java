/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.publisher.task;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;

/**
 * @author arussel
 */
public class PublishUnrestricted extends UnrestrictedSessionRunner {
    private DocumentModel newProxy;

    private final DocumentModel docToPublish;

    private final DocumentModel sectionToPublishTo;

    private final boolean overwriteProxy;

    public PublishUnrestricted(CoreSession session, DocumentModel docToPublish,
            DocumentModel sectionToPublishTo) {
        this(session, docToPublish, sectionToPublishTo, true);
    }

    public PublishUnrestricted(CoreSession session, DocumentModel docToPublish,
            DocumentModel sectionToPublishTo, boolean overwriteProxy) {
        super(session);
        this.sectionToPublishTo = sectionToPublishTo;
        this.docToPublish = docToPublish;
        this.overwriteProxy = overwriteProxy;
    }

    @Override
    public void run() throws ClientException {
        newProxy = session.publishDocument(docToPublish, sectionToPublishTo, overwriteProxy);
        session.save();
    }

    public DocumentModel getModel() {
        return newProxy;
    }

}
