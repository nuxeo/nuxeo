package org.nuxeo.snapshot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.model.PropertyException;

public class SnapshotableAdapter implements Snapshot, Serializable {

    private static final long serialVersionUID = 1L;

    protected DocumentModel doc;

    public static final String SCHEMA = "Snapshot";

    public static final String CHILDREN_PROP = "snap:children";

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
        if (targetDoc.isFolder() && !targetDoc.hasSchema("snapshot")) {
            throw new ClientException(
                    "Can not version a folder that has not snapshot schema");
        }
        if (!targetDoc.isFolder() && !targetDoc.isCheckedOut()) {
            return targetDoc.getCoreSession().getLastDocumentVersionRef(
                    targetDoc.getRef());
        }
        return targetDoc.getCoreSession().checkIn(targetDoc.getRef(), option,
                null);
    }

    protected DocumentModel createLeafVersionAndFetch(VersioningOption option)
            throws ClientException {
        DocumentRef versionRef = createLeafVersion(doc, option);

        DocumentModel version = doc.getCoreSession().getDocument(versionRef);

        if (version.isFolder() && !version.hasSchema("snapshot")) {
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

        if (!doc.hasSchema("snapshot")) {
            throw new ClientException("snapshot schema not added !");
        }

        DocumentModelList children = doc.getCoreSession().getChildren(
                doc.getRef());

        /*
         * if (children.size() == 0) { doc =
         * doc.getCoreSession().saveDocument(doc); return new
         * SnapshotableAdapter(createLeafVersionAndFetch(option)); }
         */

        String[] vuuids = new String[children.size()];

        for (int i = 0; i < children.size(); i++) {
            DocumentModel child = children.get(i);
            if (!child.isFolder()) {
                vuuids[i] = createLeafVersion(child, option).toString();
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
            doc = doc.getCoreSession().saveDocument(doc);
            return new SnapshotableAdapter(createLeafVersionAndFetch(option));
        } else {
            DocumentModel lastversion = doc.getCoreSession().getLastDocumentVersion(
                    doc.getRef());
            return new SnapshotableAdapter(lastversion);
        }
    }

    @Override
    public List<DocumentModel> getChildren() throws ClientException {

        if (!doc.isVersion()) {
            throw new ClientException("Not a version:");
        }

        if (!doc.isFolder()) {
            return Collections.emptyList();
        }

        if (doc.isFolder() && !doc.hasSchema("snapshot")) {
            throw new ClientException(
                    "Folderish children should have the snapshot schema");
        }

        try {

            String[] uuids = (String[]) doc.getPropertyValue(CHILDREN_PROP);

            if (uuids != null && uuids.length > 0) {
                DocumentRef[] refs = new DocumentRef[uuids.length];
                for (int i = 0; i < uuids.length; i++) {
                    refs[i] = new IdRef(uuids[i]);
                }
                return doc.getCoreSession().getDocuments(refs);
            }
        } catch (PropertyException e) {
            e.printStackTrace();
        }

        return Collections.emptyList();
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

    @Override
    public DocumentModel restore() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

}
