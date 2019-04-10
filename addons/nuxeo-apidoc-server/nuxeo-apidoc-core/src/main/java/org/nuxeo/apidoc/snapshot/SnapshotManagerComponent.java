/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.snapshot;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelWriter;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentTreeReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveWriter;
import org.nuxeo.runtime.model.DefaultComponent;

public class SnapshotManagerComponent extends DefaultComponent implements
        SnapshotManager {

    protected DistributionSnapshot runtimeSnapshot;

    public static final String RUNTIME = "current";

    public static final String RUNTIME_ADM = "adm";

    protected static final Log log = LogFactory.getLog(SnapshotManagerComponent.class);

    protected final SnapshotPersister persister = new SnapshotPersister();

    @Override
    public DistributionSnapshot getRuntimeSnapshot() {
        if (runtimeSnapshot == null) {
            runtimeSnapshot = new RuntimeSnapshot();
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
    public List<DistributionSnapshot> readPersistentSnapshots(
            CoreSession session) {
        List<DistributionSnapshot> snaps = RepositoryDistributionSnapshot.readPersistentSnapshots(session);
        return snaps;
    }

    public List<DistributionSnapshot> listPersistentSnapshots(
            CoreSession session) {

        List<DistributionSnapshot> distribs = readPersistentSnapshots(session);

        Collections.sort(distribs, new Comparator<DistributionSnapshot>() {
            @Override
            public int compare(DistributionSnapshot dist0,
                    DistributionSnapshot dist1) {
                if (dist0.getVersion().equals(dist1.getVersion())) {
                    return dist0.getName().compareTo(dist1.getName());
                } else {
                    return -dist0.getVersion().compareTo(dist1.getVersion());
                }
            }
        });

        return distribs;
    }

    @Override
    public Map<String, DistributionSnapshot> getPersistentSnapshots(
            CoreSession session) {

        Map<String, DistributionSnapshot> persistentSnapshots = new HashMap<String, DistributionSnapshot>();

        for (DistributionSnapshot snap : readPersistentSnapshots(session)) {
            persistentSnapshots.put(snap.getKey(), snap);
        }

        return persistentSnapshots;
    }

    @Override
    public List<String> getPersistentSnapshotNames(CoreSession session) {
        List<String> names = new ArrayList<String>();
        names.addAll(getPersistentSnapshots(session).keySet());
        return names;
    }

    @Override
    public List<DistributionSnapshotDesc> getAvailableDistributions(
            CoreSession session) {
        List<DistributionSnapshotDesc> names = new ArrayList<DistributionSnapshotDesc>();
        names.addAll(getPersistentSnapshots(session).values());
        names.add(0, getRuntimeSnapshot());
        return names;
    }

    @Override
    public DistributionSnapshot persistRuntimeSnapshot(CoreSession session)
            throws ClientException {
        return persistRuntimeSnapshot(session, null);
    }

    @Override
    public DistributionSnapshot persistRuntimeSnapshot(CoreSession session,
            String name) throws ClientException {
        return persistRuntimeSnapshot(session, name, null);
    }

    @Override
    public DistributionSnapshot persistRuntimeSnapshot(CoreSession session,
            String name, SnapshotFilter filter) throws ClientException {
        DistributionSnapshot liveSnapshot = getRuntimeSnapshot();
        DistributionSnapshot snap = persister.persist(liveSnapshot, session,
                name, filter);
        addPersistentSnapshot(snap.getKey(), snap);
        return snap;
    }

    @Override
    public List<String> getAvailableVersions(CoreSession session,
            NuxeoArtifact nxItem) {
        List<String> versions = new ArrayList<String>();

        List<DistributionSnapshot> distribs = new ArrayList<DistributionSnapshot>();
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
    public void exportSnapshot(CoreSession session, String key, OutputStream out)
            throws Exception {

        DistributionSnapshot snap = getSnapshot(key, session);

        if (snap == null) {
            throw new Exception("Unable to find Snapshot " + key);
        }

        if (snap.isLive()) {
            throw new Exception(
                    "Can not export a live distribution snapshot : " + key);
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
    public void importSnapshot(CoreSession session, InputStream is)
            throws Exception {
        try {
            String importPath = persister.getDistributionRoot(session).getPathAsString();
            DocumentReader reader = new NuxeoArchiveReader(is);
            DocumentWriter writer = new DocumentModelWriter(session, importPath);

            DocumentPipe pipe = new DocumentPipeImpl(10);
            pipe.setReader(reader);
            pipe.setWriter(writer);
            pipe.run();
            reader.close();
            writer.close();
        } catch (Exception e) {
            log.error("Error while importing snapshot", e);
        }
    }

    @Override
    public void initSeamContext(HttpServletRequest request) {
        ((RuntimeSnapshot) getRuntimeSnapshot()).initSeamComponents(request);
    }

    @Override
    public void addPersistentSnapshot(String key, DistributionSnapshot snapshot) {
        // NOP
    }

}
