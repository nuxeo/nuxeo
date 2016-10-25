/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.admin.repo;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
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

    public StatsTaskRunner(String repositoryName, boolean includeBlob, DocumentRef rootref, StatsTask hostTask) {
        super(repositoryName);
        this.includeBlob = includeBlob;
        this.rootref = rootref;
        this.hostTask = hostTask;
    }

    private void recurse(DocumentModel doc) {
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

    private void fetchInfoFromDoc(CoreSession session, DocumentModel doc) {

        if (includeBlob) {
            BlobHolder bh = doc.getAdapter(BlobHolder.class);
            if (bh != null) {
                List<Blob> blobs = bh.getBlobs();
                if (blobs != null) {
                    for (Blob blob : blobs) {
                        if (blob != null) {
                            hostTask.getInfo().addBlob(blob.getLength(), doc.getPath());
                        }
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
    public void run() {
        DocumentModel root = session.getDocument(rootref);
        recurse(root);
    }

}
