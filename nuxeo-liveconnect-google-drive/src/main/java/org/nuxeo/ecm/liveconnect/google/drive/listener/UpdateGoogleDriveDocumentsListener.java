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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ecm.liveconnect.google.drive.listener;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.liveconnect.google.drive.worker.UpdateGoogleDriveDocumentsWork;
import org.nuxeo.runtime.api.Framework;

public class UpdateGoogleDriveDocumentsListener implements PostCommitEventListener {

    @Override
    public void handleEvent(EventBundle events) throws ClientException {
        final RepositoryManager repositoryManager = Framework.getLocalService(RepositoryManager.class);
        final WorkManager workManager = Framework.getLocalService(WorkManager.class);
        for (String repositoryName : repositoryManager.getRepositoryNames()) {
            UpdateGoogleDriveDocumentsWork work = new UpdateGoogleDriveDocumentsWork();
            work.setDocument(repositoryName, null);
            workManager.schedule(work, WorkManager.Scheduling.IF_NOT_SCHEDULED, true);
        }
    }

}
