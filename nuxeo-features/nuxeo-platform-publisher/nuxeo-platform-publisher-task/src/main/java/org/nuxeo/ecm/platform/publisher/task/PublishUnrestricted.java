/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.publisher.task;

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

    public PublishUnrestricted(CoreSession session, DocumentModel docToPublish, DocumentModel sectionToPublishTo) {
        this(session, docToPublish, sectionToPublishTo, true);
    }

    public PublishUnrestricted(CoreSession session, DocumentModel docToPublish, DocumentModel sectionToPublishTo,
            boolean overwriteProxy) {
        super(session);
        this.sectionToPublishTo = sectionToPublishTo;
        this.docToPublish = docToPublish;
        this.overwriteProxy = overwriteProxy;
    }

    @Override
    public void run() {
        newProxy = session.publishDocument(docToPublish, sectionToPublishTo, overwriteProxy);
        session.save();
    }

    public DocumentModel getModel() {
        return newProxy;
    }

}
