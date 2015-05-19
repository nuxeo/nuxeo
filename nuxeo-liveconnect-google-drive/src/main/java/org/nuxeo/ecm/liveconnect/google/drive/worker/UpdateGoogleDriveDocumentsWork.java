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

package org.nuxeo.ecm.liveconnect.google.drive.worker;

import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.liveconnect.google.drive.GoogleDriveBlobProvider;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
import org.nuxeo.runtime.api.Framework;

public class UpdateGoogleDriveDocumentsWork extends AbstractWork {

    private static final Log log = LogFactory.getLog(UpdateGoogleDriveDocumentsWork.class);

    private static final long serialVersionUID = 1L;

    protected static final String TITLE = "Update Google Drive Documents Work";

    protected static final String QUERY = String.format(
            "SELECT * FROM Document WHERE content/data LIKE '%s:%%' ORDER BY ecm:uuid ASC",
            GoogleDriveBlobProvider.PREFIX);

    protected long offset = 0;

    protected static final long MAX_RESULT = 50;

    public UpdateGoogleDriveDocumentsWork() {
        super();
    }

    public UpdateGoogleDriveDocumentsWork(final long offset) {
        this();
        this.offset = offset;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public String getId() {
        return repositoryName + ":" + offset;
    }

    @Override
    public void work() {
        setStatus("Updating");
        final List<DocumentModel> results = getNextResults();
        final int nbResult = results.size();
        setProgress(new Progress(0, nbResult));
        for (DocumentModel doc : getNextResults()) {
            log.trace("Updating " + doc.getTitle());
            try {
                if (getGoogleDriveBlobProvider().updateBlob(doc)) {
                    session.saveDocument(doc);
                }
            } catch (IOException e) {
                log.error("Could not update google drive document " + doc.getTitle(), e);
            }
            log.trace("Updating done " + doc.getTitle());
        }
        if (nbResult == MAX_RESULT) {
            setStatus("Rescheduling next work");
            final UpdateGoogleDriveDocumentsWork nextWork = new UpdateGoogleDriveDocumentsWork(offset + MAX_RESULT);
            nextWork.setDocument(repositoryName, null);
            final WorkManager workManager = Framework.getLocalService(WorkManager.class);
            workManager.schedule(nextWork, WorkManager.Scheduling.IF_NOT_SCHEDULED, true);
            setStatus("Rescheduling Done");
        }
        setStatus("Done");
    }

    private List<DocumentModel> getNextResults() throws ClientException {
        List<DocumentModel> results;
        Object[] parameters = new Object[1];
        parameters[0] = docId;

        String query = NXQLQueryBuilder.getQuery(QUERY, null, false, false, null);
        if (session == null) {
            initSession();
        }
        results = session.query(query, null, MAX_RESULT, offset, MAX_RESULT);
        return results;
    }

    protected GoogleDriveBlobProvider getGoogleDriveBlobProvider() {
        return (GoogleDriveBlobProvider) Framework.getService(BlobManager.class)
            .getBlobProvider(GoogleDriveBlobProvider.PREFIX);
    }

}
