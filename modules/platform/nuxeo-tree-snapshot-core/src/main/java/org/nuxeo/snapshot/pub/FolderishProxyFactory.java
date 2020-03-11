/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.snapshot.pub;

import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.impl.core.SimpleCorePublishedDocument;
import org.nuxeo.ecm.platform.publisher.task.CoreProxyWithWorkflowFactory;
import org.nuxeo.snapshot.Snapshot;
import org.nuxeo.snapshot.Snapshotable;

public class FolderishProxyFactory extends CoreProxyWithWorkflowFactory {

    protected DocumentModel subPublish(CoreSession session, DocumentModel parentProxy, Snapshot tree, boolean skipParent)
            {

        DocumentModel newFolderishProxy = null;
        if (skipParent) {
            newFolderishProxy = parentProxy;
        } else {
            DocumentModel version = tree.getDocument();
            newFolderishProxy = session.createProxy(version.getRef(), parentProxy.getRef());
        }

        for (Snapshot snap : tree.getChildrenSnapshots()) {
            subPublish(session, newFolderishProxy, snap, false);
        }

        return newFolderishProxy;
    }

    @Override
    public PublishedDocument publishDocument(DocumentModel doc, PublicationNode targetNode, Map<String, String> params)
            {

        Snapshot snapshot = null;

        if (doc.isFolder()) {
            Snapshotable snapshotable = doc.getAdapter(Snapshotable.class);
            if (snapshotable != null) {
                snapshot = snapshotable.createSnapshot(VersioningOption.MINOR);
            }
        }

        PublishedDocument result = super.publishDocument(doc, targetNode, params);

        if (snapshot != null) {

            final Snapshot tree = snapshot;
            final DocumentModel parent = ((SimpleCorePublishedDocument) result).getProxy();

            UnrestrictedSessionRunner runner = new UnrestrictedSessionRunner(doc.getCoreSession()) {
                @Override
                public void run() {
                    // force cleanup of the tree !!!
                    session.removeChildren(parent.getRef());
                    subPublish(session, parent, tree, true);
                }
            };
            runner.runUnrestricted();
        }

        return result;

    }
}
