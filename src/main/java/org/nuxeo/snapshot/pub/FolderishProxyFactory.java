package org.nuxeo.snapshot.pub;

import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
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

    protected DocumentModel subPublish(CoreSession session,
            DocumentModel parentProxy, Snapshot tree, boolean skipParent)
            throws ClientException {

        DocumentModel newFolderishProxy = null;
        if (skipParent) {
            newFolderishProxy = parentProxy;
        } else {
            DocumentModel version = tree.getDocument();
            newFolderishProxy = session.createProxy(version.getRef(),
                    parentProxy.getRef());
        }

        for (Snapshot snap : tree.getChildrenSnapshots()) {
            subPublish(session, newFolderishProxy, snap, false);
        }

        return newFolderishProxy;
    }

    @Override
    public PublishedDocument publishDocument(DocumentModel doc,
            PublicationNode targetNode, Map<String, String> params)
            throws ClientException {

        Snapshot snapshot = null;

        if (doc.isFolder()) {
            Snapshotable snapshotable = doc.getAdapter(Snapshotable.class);
            if (snapshotable != null) {
                snapshot = snapshotable.createSnapshot(VersioningOption.MINOR);
            }
        }

        PublishedDocument result = super.publishDocument(doc, targetNode,
                params);

        if (snapshot != null) {

            final Snapshot tree = snapshot;
            final DocumentModel parent = ((SimpleCorePublishedDocument) result).getProxy();

            UnrestrictedSessionRunner runner = new UnrestrictedSessionRunner(
                    doc.getCoreSession()) {
                @Override
                public void run() throws ClientException {
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
