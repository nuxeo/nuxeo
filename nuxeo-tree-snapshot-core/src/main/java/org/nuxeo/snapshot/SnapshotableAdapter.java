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
package org.nuxeo.snapshot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.runtime.api.Framework;

public class SnapshotableAdapter implements Snapshot, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(SnapshotableAdapter.class);

    protected DocumentModel doc;

    public static final String SCHEMA = "snapshot";

    public static final String CHILDREN_PROP = "snap:children";

    public static final String NAME_PROP = "snap:originalName";

    public SnapshotableAdapter(DocumentModel doc) {
        this.doc = doc;
    }

    @Override
    public DocumentModel getDocument() {
        return doc;
    }

    @Override
    public DocumentRef getRef() {
        return doc.getRef();
    }

    protected DocumentRef createLeafVersion(DocumentModel targetDoc, VersioningOption option) {
        if (targetDoc.isFolder() && !targetDoc.hasSchema(SCHEMA)) {
            throw new NuxeoException("Can not version a folder that has not snapshot schema");
        }
        if (targetDoc.isVersion()) {
            return targetDoc.getRef();
        }
        if (!targetDoc.isProxy() && !targetDoc.isCheckedOut()) {
            return targetDoc.getCoreSession().getBaseVersion(targetDoc.getRef());
        }
        if (targetDoc.isProxy()) {
            DocumentModel proxyTarget = targetDoc.getCoreSession().getDocument(new IdRef(targetDoc.getSourceId()));
            if (proxyTarget.isVersion()) {
                // standard proxy : nothing to snapshot
                return targetDoc.getRef();
            } else {
                // live proxy
                // create a new leaf with target doc ?
                return createLeafVersion(proxyTarget, option);

                // create a new proxy ??
                // XXX
            }

        }

        // Fire event to change document
        DocumentEventContext ctx = new DocumentEventContext(targetDoc.getCoreSession(),
                targetDoc.getCoreSession().getPrincipal(), targetDoc);
        ctx.setProperty(ROOT_DOCUMENT_PROPERTY, doc);

        Framework.getService(EventService.class).fireEvent(ctx.newEvent(ABOUT_TO_CREATE_LEAF_VERSION_EVENT));
        // Save only if needed
        if (targetDoc.isDirty()) {
            targetDoc.getCoreSession().saveDocument(targetDoc);
        }

        return targetDoc.getCoreSession().checkIn(targetDoc.getRef(), option, null);
    }

    protected DocumentModel createLeafVersionAndFetch(VersioningOption option) {
        DocumentRef versionRef = createLeafVersion(doc, option);
        DocumentModel version = doc.getCoreSession().getDocument(versionRef);
        return version;
    }

    @Override
    public Snapshot createSnapshot(VersioningOption option) {

        if (!doc.isFolder()) {
            if (doc.isCheckedOut()) {
                return new SnapshotableAdapter(createLeafVersionAndFetch(option));
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

        List<DocumentModel> folders = new ArrayList<>();
        List<DocumentModel> leafs = new ArrayList<>();
        String[] vuuids = null;

        doc.getCoreSession().save();
        String query = "SELECT ecm:uuid FROM Document WHERE ecm:parentId = '" + doc.getId()
                + "' AND ecm:isTrashed = 0 ORDER BY ecm:pos";
        try (IterableQueryResult res = doc.getCoreSession().queryAndFetch(query, "NXQL")) {

            vuuids = new String[(int) res.size()];
            for (Map<String, Serializable> item : res) {
                DocumentModel child = doc.getCoreSession().getDocument(new IdRef((String) item.get(NXQL.ECM_UUID)));
                if (child.isFolder()) {
                    folders.add(child);
                } else {
                    leafs.add(child);
                }
            }

        }

        int i = 0;

        for (DocumentModel child : leafs) {
            DocumentRef docRef = createLeafVersion(child, option);
            String versionUuid = docRef.toString();
            vuuids[i++] = versionUuid;
        }

        for (DocumentModel child : folders) {
            SnapshotableAdapter adapter = new SnapshotableAdapter(child);
            Snapshot snap = adapter.createSnapshot(option);
            String versionUuid = snap.getRef().toString();
            vuuids[i++] = versionUuid;
        }

        // check if a snapshot is needed
        boolean mustSnapshot = false;
        if (doc.isCheckedOut()) {
            mustSnapshot = true;
        } else {
            String[] existingUUIds = (String[]) doc.getPropertyValue(CHILDREN_PROP);
            if (existingUUIds == null) {
                existingUUIds = new String[0];
            }
            if (doc.hasFacet(FacetNames.ORDERABLE)) {
                // ordered, exact comparison
                if (!Arrays.equals(vuuids, existingUUIds)) {
                    mustSnapshot = true;
                }
            } else {
                // not ordered, use unordered comparison
                if (!new HashSet<>(Arrays.asList(vuuids)).equals(new HashSet<>(Arrays.asList(existingUUIds)))) {
                    mustSnapshot = true;
                }
            }
        }

        if (mustSnapshot) {
            doc.setPropertyValue(CHILDREN_PROP, vuuids);
            doc.setPropertyValue(NAME_PROP, doc.getName());
            doc = doc.getCoreSession().saveDocument(doc);
            return new SnapshotableAdapter(createLeafVersionAndFetch(option));
        } else {
            DocumentModel lastversion = doc.getCoreSession().getLastDocumentVersion(doc.getRef());
            return new SnapshotableAdapter(lastversion);
        }
    }

    protected List<DocumentModel> getChildren(DocumentModel target) {
        if (!target.isVersion()) {
            throw new NuxeoException("Not a version:");
        }

        if (!target.isFolder()) {
            return Collections.emptyList();
        }

        if (target.isFolder() && !target.hasSchema(SCHEMA)) {
            throw new NuxeoException("Folderish children should have the snapshot schema");
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
            log.error(e, e);
        }

        return Collections.emptyList();
    }

    @Override
    public List<DocumentModel> getChildren() {
        return getChildren(doc);
    }

    @Override
    public List<Snapshot> getChildrenSnapshots() {

        List<Snapshot> snaps = new ArrayList<Snapshot>();

        for (DocumentModel child : getChildren()) {
            snaps.add(new SnapshotableAdapter(child));
        }

        return snaps;
    }

    protected void fillFlatTree(List<Snapshot> list) {
        for (Snapshot snap : getChildrenSnapshots()) {
            list.add(snap);
            if (snap.getDocument().isFolder()) {
                ((SnapshotableAdapter) snap).fillFlatTree(list);
            }
        }
    }

    @Override
    public List<Snapshot> getFlatTree() {
        List<Snapshot> list = new ArrayList<Snapshot>();

        fillFlatTree(list);

        return list;
    }

    protected void dump(int level, StringBuffer sb) {
        for (Snapshot snap : getChildrenSnapshots()) {
            sb.append(new String(new char[level]).replace('\0', ' '));
            sb.append(snap.getDocument().getName() + " -- " + snap.getDocument().getVersionLabel());
            sb.append("\n");
            if (snap.getDocument().isFolder()) {
                ((SnapshotableAdapter) snap).dump(level + 1, sb);
            }
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

    protected DocumentModel getVersionForLabel(DocumentModel target, String versionLabel) {
        List<DocumentModel> versions = target.getCoreSession().getVersions(target.getRef());
        for (DocumentModel version : versions) {
            if (version.getVersionLabel().equals(versionLabel)) {
                return version;
            }
        }
        return null;
    }

    protected DocumentModel getCheckoutDocument(DocumentModel target) {
        if (target.isVersion()) {
            target = target.getCoreSession().getDocument(new IdRef(doc.getSourceId()));
        }
        return target;
    }

    protected DocumentModel restore(DocumentModel leafVersion, DocumentModel target, boolean first,
            DocumentModelList olddocs) {

        CoreSession session = doc.getCoreSession();

        if (leafVersion == null) {
            return null;
        }

        if (target.isFolder() && first) {
            // save all subtree
            olddocs = session.query(
                    "select * from Document where ecm:path STARTSWITH " + NXQL.escapeString(target.getPathAsString()));
            if (olddocs.size() > 0) {
                DocumentModel container = session.createDocumentModel(target.getPath().removeLastSegments(1).toString(),
                        target.getName() + "_tmp", "Folder");
                container = session.createDocument(container);
                for (DocumentModel oldChild : olddocs) {
                    session.move(oldChild.getRef(), container.getRef(), oldChild.getName());
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
                session.move(placeholder.getRef(), target.getRef(), placeholder.getName());
            } else {
                String name = child.getName();
                // name will be null if there is no checkecout version
                // need to rebuild name
                if (name == null && child.hasSchema(SCHEMA)) {
                    name = (String) child.getPropertyValue(NAME_PROP);
                }
                if (name == null && child.getTitle() != null) {
                    name = IdUtils.generateId(child.getTitle(), "-", true, 24);
                }
                if (name == null) {
                    name = child.getType() + System.currentTimeMillis();
                }
                placeholder = new DocumentModelImpl((String) null, child.getType(), liveUUID, new Path(name), null,
                        null, target.getRef(), null, null, null, null);
                placeholder.putContextData(CoreSession.IMPORT_CHECKED_IN, Boolean.TRUE);
                placeholder.addFacet(Snapshot.FACET);
                placeholder.addFacet(FacetNames.VERSIONABLE);
                session.importDocuments(Collections.singletonList(placeholder));
                placeholder = session.getDocument(new IdRef(liveUUID));
            }

            new SnapshotableAdapter(child).restore(child, placeholder, false, olddocs);
        }

        if (first) {
            for (DocumentModel old : olddocs) {
                session.removeDocument(old.getRef());
            }
        }
        return target;
    }

    @Override
    public DocumentModel restore(String versionLabel) {
        DocumentModel target = getCheckoutDocument(doc);
        DocumentModel leafVersion = getVersionForLabel(target, versionLabel);
        DocumentModel restoredDoc = restore(leafVersion, target, true, null);
        return restoredDoc;
    }

}
