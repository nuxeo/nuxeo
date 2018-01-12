/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.apidoc.snapshot;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.introspection.RuntimeSnapshot;
import org.nuxeo.apidoc.repository.RepositoryDistributionSnapshot;
import org.nuxeo.apidoc.repository.SnapshotPersister;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelWriter;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentTreeReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveWriter;
import org.nuxeo.runtime.model.DefaultComponent;

import static java.util.Collections.reverseOrder;
import static java.util.Comparator.comparing;
import static org.nuxeo.ecm.core.api.validation.DocumentValidationService.CTX_MAP_KEY;
import static org.nuxeo.ecm.core.api.validation.DocumentValidationService.Forcing.TURN_OFF;

public class SnapshotManagerComponent extends DefaultComponent implements SnapshotManager {

    protected volatile DistributionSnapshot runtimeSnapshot;

    public static final String RUNTIME = "current";

    public static final String RUNTIME_ADM = "adm";

    protected static final String IMPORT_TMP = "tmpImport";

    protected static final Log log = LogFactory.getLog(SnapshotManagerComponent.class);

    protected final SnapshotPersister persister = new SnapshotPersister();

    @Override
    public DistributionSnapshot getRuntimeSnapshot() {
        if (runtimeSnapshot == null) {
            synchronized (this) {
                if (runtimeSnapshot == null) {
                    runtimeSnapshot = RuntimeSnapshot.build();
                }
            }
        }
        return runtimeSnapshot;
    }

    @Override
    public DistributionSnapshot getSnapshot(String key, CoreSession session) {
        if (key == null || RUNTIME.equals(key) || RUNTIME_ADM.equals(key)) {
            return getRuntimeSnapshot();
        }
        DistributionSnapshot snap = getPersistentSnapshots(session).get(key);
        if (snap == null) {
            DistributionSnapshot rtsnap = getRuntimeSnapshot();
            if (rtsnap.getKey().equals(key)) {
                return rtsnap;
            }
        }
        return snap;
    }

    @Override
    public List<DistributionSnapshot> readPersistentSnapshots(CoreSession session) {
        return RepositoryDistributionSnapshot.readPersistentSnapshots(session);
    }

    @Override
    public List<DistributionSnapshot> listPersistentSnapshots(CoreSession session) {

        List<DistributionSnapshot> distribs = readPersistentSnapshots(session);

        Collections.sort(distribs,
                reverseOrder(comparing(DistributionSnapshot::getVersion)).thenComparing(DistributionSnapshot::getName));

        return distribs;
    }

    @Override
    public Map<String, DistributionSnapshot> getPersistentSnapshots(CoreSession session) {

        Map<String, DistributionSnapshot> persistentSnapshots = new HashMap<>();

        for (DistributionSnapshot snap : readPersistentSnapshots(session)) {
            persistentSnapshots.put(snap.getKey(), snap);
        }

        return persistentSnapshots;
    }

    @Override
    public List<String> getPersistentSnapshotNames(CoreSession session) {
        List<String> names = new ArrayList<>();
        names.addAll(getPersistentSnapshots(session).keySet());
        return names;
    }

    @Override
    public List<DistributionSnapshotDesc> getAvailableDistributions(CoreSession session) {
        List<DistributionSnapshotDesc> names = new ArrayList<>();
        names.addAll(getPersistentSnapshots(session).values());
        names.add(0, getRuntimeSnapshot());
        return names;
    }

    @Override
    public DistributionSnapshot persistRuntimeSnapshot(CoreSession session) {
        return persistRuntimeSnapshot(session, null, null);
    }

    @Override
    public DistributionSnapshot persistRuntimeSnapshot(CoreSession session, String name,
            Map<String, Serializable> properties) {
        return persistRuntimeSnapshot(session, name, properties, null);
    }

    @Override
    public DistributionSnapshot persistRuntimeSnapshot(CoreSession session, String name,
            Map<String, Serializable> properties, SnapshotFilter filter) {
        DistributionSnapshot liveSnapshot = getRuntimeSnapshot();
        DistributionSnapshot snap = persister.persist(liveSnapshot, session, name, filter, properties);
        addPersistentSnapshot(snap.getKey(), snap);
        return snap;
    }

    @Override
    public List<String> getAvailableVersions(CoreSession session, NuxeoArtifact nxItem) {
        List<String> versions = new ArrayList<>();

        List<DistributionSnapshot> distribs = new ArrayList<>();
        distribs.addAll(getPersistentSnapshots(session).values());
        distribs.add(getRuntimeSnapshot());

        for (DistributionSnapshot snap : distribs) {

            String version = null;
            if (BundleGroup.TYPE_NAME.equals(nxItem.getArtifactType())) {
                BundleGroup bg = snap.getBundleGroup(nxItem.getId());
                if (bg != null) {
                    version = bg.getVersion();
                }
            } else if (BundleInfo.TYPE_NAME.equals(nxItem.getArtifactType())) {
                BundleInfo bi = snap.getBundle(nxItem.getId());
                if (bi != null) {
                    version = bi.getVersion();
                }
            } else if (ComponentInfo.TYPE_NAME.equals(nxItem.getArtifactType())) {
                ComponentInfo ci = snap.getComponent(nxItem.getId());
                if (ci != null) {
                    version = ci.getVersion();
                }
            } else if (ExtensionInfo.TYPE_NAME.equals(nxItem.getArtifactType())) {
                ExtensionInfo ei = snap.getContribution(nxItem.getId());
                if (ei != null) {
                    version = ei.getVersion();
                }
            } else if (ExtensionPointInfo.TYPE_NAME.equals(nxItem.getArtifactType())) {
                ExtensionPointInfo epi = snap.getExtensionPoint(nxItem.getId());
                if (epi != null) {
                    version = epi.getVersion();
                }
            } else if (ServiceInfo.TYPE_NAME.equals(nxItem.getArtifactType())) {
                ServiceInfo si = snap.getService(nxItem.getId());
                if (si != null) {
                    version = si.getVersion();
                }
            } else if (OperationInfo.TYPE_NAME.equals(nxItem.getArtifactType())) {
                OperationInfo oi = snap.getOperation(nxItem.getId());
                if (oi != null) {
                    version = oi.getVersion();
                }
            }

            if (version != null && !versions.contains(version)) {
                versions.add(version);
            }
        }
        return versions;
    }

    @Override
    public void exportSnapshot(CoreSession session, String key, OutputStream out) throws IOException {

        DistributionSnapshot snap = getSnapshot(key, session);

        if (snap == null) {
            throw new NuxeoException("Unable to find Snapshot " + key);
        }

        if (snap.isLive()) {
            throw new NuxeoException("Can not export a live distribution snapshot : " + key);
        }

        RepositoryDistributionSnapshot docSnap = (RepositoryDistributionSnapshot) snap;
        DocumentModel root = docSnap.getDoc();

        DocumentReader reader = new DocumentTreeReader(session, root);
        DocumentWriter writer = new NuxeoArchiveWriter(out);
        DocumentPipe pipe = new DocumentPipeImpl(10);
        pipe.setReader(reader);
        pipe.setWriter(writer);
        pipe.run();
        reader.close();
        writer.close();
    }

    @Override
    public void importSnapshot(CoreSession session, InputStream is) throws IOException {
        String importPath = persister.getDistributionRoot(session).getPathAsString();
        DocumentReader reader = new NuxeoArchiveReader(is);
        DocumentWriter writer = new DocumentModelWriter(session, importPath);
        DocumentPipe pipe = new DocumentPipeImpl(10);
        pipe.setReader(reader);
        pipe.setWriter(writer);
        pipe.run();
        reader.close();
        writer.close();
    }

    @Override
    public void validateImportedSnapshot(CoreSession session, String name, String version, String pathSegment,
            String title) {

        DocumentModel container = persister.getDistributionRoot(session);
        DocumentRef tmpRef = new PathRef(container.getPathAsString(), IMPORT_TMP);

        DocumentModel tmp;
        if (session.exists(tmpRef)) {
            tmp = session.getChild(container.getRef(), IMPORT_TMP);
            DocumentModel snapDoc = session.getChildren(tmp.getRef()).get(0);
            snapDoc.setPropertyValue("nxdistribution:name", name);
            snapDoc.setPropertyValue("nxdistribution:version", version);
            snapDoc.setPropertyValue("nxdistribution:key", name + "-" + version);
            snapDoc.setPropertyValue("dc:title", title);
            snapDoc = session.saveDocument(snapDoc);

            DocumentModel targetContainer = session.getParentDocument(tmp.getRef());

            session.move(snapDoc.getRef(), targetContainer.getRef(), pathSegment);
            session.removeDocument(tmp.getRef());
        }

    }

    @Override
    public DocumentModel importTmpSnapshot(CoreSession session, InputStream is) throws IOException {
        DocumentModel container = persister.getDistributionRoot(session);
        DocumentRef tmpRef = new PathRef(container.getPathAsString(), IMPORT_TMP);

        DocumentModel tmp;
        if (session.exists(tmpRef)) {
            tmp = session.getChild(container.getRef(), IMPORT_TMP);
            session.removeChildren(tmp.getRef());
        } else {
            tmp = session.createDocumentModel(container.getPathAsString(), IMPORT_TMP, "Workspace");
            tmp.setPropertyValue("dc:title", "tmpImport");
            tmp = session.createDocument(tmp);
            session.save();
        }

        DocumentReader reader = new NuxeoArchiveReader(is);
        DocumentWriter writer = new SnapshotWriter(session, tmp.getPathAsString());

        DocumentPipe pipe = new DocumentPipeImpl(10);
        pipe.setReader(reader);
        pipe.setWriter(writer);
        pipe.run();
        reader.close();
        writer.close();

        return session.getChildren(tmp.getRef()).get(0);
    }

    @Override
    public void initSeamContext(HttpServletRequest request) {
        ((RuntimeSnapshot) getRuntimeSnapshot()).initSeamComponents(request);
    }

    @Override
    public void addPersistentSnapshot(String key, DistributionSnapshot snapshot) {
        // NOP
    }

    /**
     * Custom Write to disable Validation Service
     */
    protected static class SnapshotWriter extends DocumentModelWriter {
        public SnapshotWriter(CoreSession session, String parentPath) {
            super(session, parentPath);
        }

        @Override
        protected void beforeCreateDocument(DocumentModel doc) {
            doc.putContextData(CTX_MAP_KEY, TURN_OFF);
        }
    }

}
