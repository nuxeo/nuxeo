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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.picture.listener;

import static org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants.UPDATE_PICTURE_VIEW_EVENT;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitFilteringEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.picture.PictureViewsGenerationWork;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener generating picture views.
 *
 * @since 5.7.2
 */
public class PictureViewListener implements PostCommitFilteringEventListener {
    @Override
    public void handleEvent(EventBundle events) throws ClientException {
        for (Event event : events) {
            if (UPDATE_PICTURE_VIEW_EVENT.equals(event.getName())) {
                handleEvent(event);
            }
        }
    }

    private void handleEvent(Event event) throws ClientException {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }

        DocumentEventContext docCtx = (DocumentEventContext) ctx;
        DocumentModel doc = docCtx.getSourceDocument();

        // launch work doing the actual views generation
        PictureViewsGenerationWork work = new PictureViewsGenerationWork(
                doc.getRepositoryName(), doc.getId(), "file:content");
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        workManager.schedule(work, WorkManager.Scheduling.IF_NOT_SCHEDULED,
                true);
    }

    @Override
    public boolean acceptEvent(Event event) {
        return UPDATE_PICTURE_VIEW_EVENT.equals(event.getName());
    }
}
