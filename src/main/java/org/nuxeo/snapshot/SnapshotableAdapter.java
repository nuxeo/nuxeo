package org.nuxeo.snapshot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.dom4j.swing.LeafTreeNode;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.schema.FacetNames;

public class SnapshotableAdapter implements Snapshot, Serializable {

    private static final long serialVersionUID = 1L;

    protected DocumentModel doc;

    public static final String SCHEMA = "snapshot";

    public static final String CHILDREN_PROP = "snap:children";

    public static final String NAME_PROP = "snap:originalName";

    public SnapshotableAdapter(DocumentModel doc) {
        this.doc = doc;
    }

    public DocumentModel getDocument() {
        return doc;
    }

    public DocumentRef getRef() {
        return doc.getRef();
    }

    protected DocumentRef createLeafVersion(DocumentModel targetDoc,
            VersioningOption option) throws ClientException {
        if (targetDoc.isFolder() && !targetDoc.hasSchema(SCHEMA)) {
            throw new ClientException(
                    "Can not version a folder that has not snapshot schema");
        }
        if (!targetDoc.isFolder() && !targetDoc.isCheckedOut()) {
            if (targetDoc.isVersion()) {
                return targetDoc.getRef();
            }
            return targetDoc.getCoreSession().getLastDocumentVersionRef(
                    targetDoc.getRef());
        }
        if (targetDoc.isVersion()) {
            return targetDoc.getRef();
        }
        if (targetDoc.isProxy()) {
            DocumentModel proxyTarget = targetDoc.getCoreSession().getDocument(
                    new IdRef(targetDoc.getSourceId()));
            if (proxyTarget.isVersion()) {
                // standard proxy : nothing to snapshot
                return targetDoc.getRef();
            } else {
                // live proxy
                // checkin the target doc ?
                targetDoc.getCoreSession().checkIn(proxyTarget.getRef(),
                        option, null);
                // create a new proxy ??
            }

        }
        return targetDoc.getCoreSession().checkIn(targetDoc.getRef(), option,
                null);
    }

    protected DocumentModel createLeafVersionAndFetch(VersioningOption option)
            throws ClientException {
        DocumentRef versionRef = createLeafVersion(doc, option);

        DocumentModel version = doc.getCoreSession().getDocument(versionRef);

        if (version.isFolder() && !version.hasSchema(SCHEMA)) {
            throw new ClientException("Error while creating version");
        }

        return version;
    }

    protected static boolean compareArrays(String[] arr1, String[] arr2) {
        HashSet<String> set1 = new HashSet<String>(Arrays.asList(arr1));
        HashSet<String> set2 = new HashSet<String>(Arrays.asList(arr2));
        return set1.equals(set2);
    }

    @Override
    public Snapshot createSnapshot(VersioningOption option)
            throws ClientException {

        if (!doc.isFolder()) {
            if (doc.isCheckedOut()) {
                return new SnapshotableAdapter(
                        createLeafVersionAndFetch(option));
            } else {
                return new SnapshotableAdapter(doc);
            }
        }

        if (!doc.hasFacet(Snapshot.FACET)) {
            doc.addFacet(Snapshot.FACET);
        }

        if (!doc.hasFacet(FacetNames.VERSIONABLE)) {
            doc.addFacet(FacetNames.VERSIONABLE);
        }

        if (!doc.hasSchema(SCHEMA)) {
            throw new ClientException("snapshot schema not added !");
        }

        DocumentModelList children = doc.getCoreSession().getChildren(
                doc.getRef());

        String[] vuuids = new String[children.size()];

        for (int i = 0; i < children.size(); i++) {
            DocumentModel child = children.get(i);
            if (!child.isFolder()) {
                DocumentRef leafRef = createLeafVersion(child, option);
                if (leafRef != null) {
                    vuuids[i] = leafRef.toString();
                } else {
                    throw new ClientException(
                            "Unable to create leaf version for "
                                    + child.getPathAsString() + " ("
                                    + child.isVersion() + "," + child.isProxy()
                                    + ")");
                }
            } else {
                SnapshotableAdapter adapter = new SnapshotableAdapter(child);
                Snapshot snap = adapter.createSnapshot(option);
                vuuids[i] = snap.getRef().toString();
            }
        }

        // check if a snapshot is needed
        boolean mustSnapshot = false;
        if (doc.isCheckedOut()) {
            mustSnapshot = true;
        } else {
            String[] existingUUIds = (String[]) doc.getPropertyValue(CHILDREN_PROP);
            if (!compareArrays(vuuids, existingUUIds)) {
                mustSnapshot = true;
            }
        }

        if (mustSnapshot) {
            doc.setPropertyValue(CHILDREN_PROP, vuuids);
            doc.setPropertyValue(NAME_PROP, doc.getName());
            doc = doc.getCoreSession().saveDocument(doc);
            return new SnapshotableAdapter(createLeafVersionAndFetch(option));
        } else {
            DocumentModel lastversion = doc.getCoreSession().getLastDocumentVersion(
                    doc.getRef());
            return new SnapshotableAdapter(lastversion);
        }
    }

    protected List<DocumentModel> getChildren(DocumentModel target)
            throws ClientException {
        if (!target.isVersion()) {
            throw new ClientException("Not a version:");
        }

        if (!target.isFolder()) {
            return Collections.emptyList();
        }

        if (target.isFolder() && !target.hasSchema(SCHEMA)) {
            throw new ClientException(
                    "Folderish children should have the snapshot schema");
        }

        try {

            String[] uuids = (String[]) target.getPropertyValue(CHILDREN_PROP);

            if (uuids != null && uuids.length > 0) {
                DocumentRef[] refs = new DocumentRef[uuids.length];
                for (int i = 0; i < uuids.length; i++) {
                    refs[i] = new IdRef(uuids[i]);
                }
                return target.getCoreSession().getDocuments(refs);
            }
        } catch (PropertyException e) {
            e.printStackTrace();
        }

        return Collections.emptyList();
    }

    @Override
    public List<DocumentModel> getChildren() throws ClientException {
        return getChildren(doc);
    }

    @Override
    public List<Snapshot> getChildrenSnapshots() throws ClientException {

        List<Snapshot> snaps = new ArrayList<Snapshot>();

        for (DocumentModel child : getChildren()) {
            snaps.add(new SnapshotableAdapter(child));
        }

        return snaps;
    }

    protected void fillFlatTree(List<Snapshot> list) throws ClientException {
        for (Snapshot snap : getChildrenSnapshots()) {
            list.add(snap);
            if (snap.getDocument().isFolder()) {
                ((SnapshotableAdapter) snap).fillFlatTree(list);
            }
        }
    }

    public List<Snapshot> getFlatTree() throws ClientException {
        List<Snapshot> list = new ArrayList<Snapshot>();

        fillFlatTree(list);

        return list;
    }

    protected void dump(int level, StringBuffer sb) {
        try {
            for (Snapshot snap : getChildrenSnapshots()) {
                sb.append(new String(new char[level]).replace('\0', ' '));
                sb.append(snap.getDocument().getName() + " -- "
                        + snap.getDocument().getVersionLabel());
                sb.append("\n");
                if (snap.getDocument().isFolder()) {
                    ((SnapshotableAdapter) snap).dump(level + 1, sb);
                }
            }
        } catch (Exception e) {
            sb.append(doc.getId() + ":" + doc.getPathAsString() + ":ERR:"
                    + e.toString() + "\n");
        }
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(doc.getName() + " -- " + doc.getVersionLabel());
        sb.append("\n");

        dump(1, sb);

        return sb.toString();
    }

    protected DocumentModel getVersionForLabel(DocumentModel target,
            String versionLabel) throws ClientException {
        List<DocumentModel> versions = target.getCoreSession().getVersions(
                target.getRef());
        for (DocumentModel version : versions) {
            if (version.getVersionLabel().equals(versionLabel)) {
                return version;
            }
        }
        return null;
    }

    protected DocumentModel getCheckoutDocument(DocumentModel target)
            throws ClientException {
        if (target.isVersion()) {
            target = target.getCoreSession().getDocument(
                    new IdRef(doc.getSourceId()));
        }
        return target;
    }

    protected DocumentModel restore(DocumentModel leafVersion,
            DocumentModel target, boolean first, DocumentModelList olddocs)
            throws ClientException {

        CoreSession session = doc.getCoreSession();

        if (leafVersion == null) {
            return null;
        }

        if (target.isFolder() && first) {
            // save all subtree
            olddocs = session.query("select * from Document where ecm:path STARTSWITH '"
                    + target.getPathAsString() + "'");
            if (olddocs.size() > 0) {
                DocumentModel container = session.createDocumentModel(
                        target.getPath().removeLastSegments(1).toString(),
                        target.getName() + "_tmp", "Folder");
                container = session.createDocument(container);
                for (DocumentModel oldChild : olddocs) {
                    session.move(oldChild.getRef(), container.getRef(),
                            oldChild.getName());
                }
                olddocs.add(container);
            }
        }

        // restore leaf
        target = session.restoreToVersion(target.getRef(), leafVersion.getRef());

        // restore children
        for (DocumentModel child : getChildren(leafVersion)) {

            String liveUUID = child.getVersionSeriesId();
            DocumentModel placeholder = null;
            for (DocumentModel doc : olddocs) {
                if (doc.getId().equals(liveUUID)) {
                    placeholder = doc;
                    break;
                }
            }
            if (placeholder == null) {
                if (session.exists(new IdRef(liveUUID))) {
                    placeholder = session.getDocument(new IdRef(liveUUID));
                }
            }
            if (placeholder != null) {
                olddocs.remove(placeholder);
                session.move(placeholder.getRef(), target.getRef(),
                        placeholder.getName());
            } else {
                String name = child.getName();
                // name will be null if there is no checkecout version
                // need to rebuild name
                if (name == null && child.hasSchema(SCHEMA)) {
                    name = (String) child.getPropertyValue(NAME_PROP);
                }
                if (name == null && child.getTitle() != null) {
                    name = IdUtils.generateId(child.getTitle(), "-", true, 24);
                    ;
                }
                if (name == null) {
                    name = child.getType() + System.currentTimeMillis();
                }
                placeholder = new DocumentModelImpl((String) null,
                        child.getType(), liveUUID, new Path(name), null, null,
                        target.getRef(), null, null, null, null);
                placeholder.putContextData(CoreSession.IMPORT_CHECKED_IN,
                        Boolean.TRUE);
                placeholder.addFacet(Snapshot.FACET);
                placeholder.addFacet(FacetNames.VERSIONABLE);
                session.importDocuments(Collections.singletonList(placeholder));
                placeholder = session.getDocument(new IdRef(liveUUID));
            }

            new SnapshotableAdapter(child).restore(child, placeholder, false,
                    olddocs);
        }

        if (first) {
            for (DocumentModel old : olddocs) {
                session.removeDocument(old.getRef());
            }
        }
        return target;
    }

    @Override
    public DocumentModel restore(String versionLabel) throws ClientException {
        DocumentModel target = getCheckoutDocument(doc);
        DocumentModel leafVersion = getVersionForLabel(target, versionLabel);
        DocumentModel restoredDoc = restore(leafVersion, target, true, null);
        return restoredDoc;
    }

}
