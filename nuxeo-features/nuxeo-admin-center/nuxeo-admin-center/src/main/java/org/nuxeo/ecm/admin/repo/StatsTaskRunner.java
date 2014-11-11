/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.admin.repo;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

/**
 * Unrestricted Runner for the statistics gathering
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
public class StatsTaskRunner extends UnrestrictedSessionRunner {

    protected static final Log log = LogFactory.getLog(StatsTaskRunner.class);

    protected final boolean includeBlob;

    protected final DocumentRef rootref;

    protected final StatsTask hostTask;

    public StatsTaskRunner(String repositoryName, boolean includeBlob,
            DocumentRef rootref, StatsTask hostTask) {
        super(repositoryName);
        this.includeBlob = includeBlob;
        this.rootref = rootref;
        this.hostTask = hostTask;
    }

    private void recurse(DocumentModel doc) throws ClientException {
        fetchInfoFromDoc(session, doc);
        if (doc.isFolder()) {
            long children = 0;
            for (DocumentModel child : session.getChildren(doc.getRef())) {
                children += 1;
                if (child.isFolder()) {
                    StatsTask newTask = hostTask.getNextTask(child);
                    if (newTask != null) {
                        hostTask.exec(newTask);
                    } else {
                        recurse(child);
                    }
                } else {
                    fetchInfoFromDoc(session, child);
                }
            }
            hostTask.getInfo().childrenCount(children, doc.getPath());
        }
    }

    private void fetchInfoFromDoc(CoreSession session, DocumentModel doc)
            throws ClientException {

        if (includeBlob) {
            BlobHolder bh = doc.getAdapter(BlobHolder.class);
            if (bh != null) {
                List<Blob> blobs = bh.getBlobs();
                if (blobs != null) {
                    for (Blob blob : blobs) {
                        hostTask.getInfo().addBlob(blob.getLength(),
                                doc.getPath());
                    }
                }
            }
        }

        if (doc.isVersion()) {
            hostTask.getInfo().addDoc(doc.getType(), doc.getPath(), true);
        } else {
            hostTask.getInfo().addDoc(doc.getType(), doc.getPath());
            List<DocumentModel> versions = session.getVersions(doc.getRef());
            for (DocumentModel version : versions) {
                fetchInfoFromDoc(session, version);
            }
        }

    }

    @Override
    public void run() throws ClientException {
        try {
            DocumentModel root = session.getDocument(rootref);
            recurse(root);
        } catch (Exception e) {
            log.error("Error while running StatsTaskRunner", e);
            throw new ClientException(e);
        }
    }

}
