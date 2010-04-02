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

package org.nuxeo.apidoc.snapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.introspection.RuntimeSnapshot;
import org.nuxeo.apidoc.repository.RepositoryDistributionSnapshot;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */
public class SnapshotManager {

    protected static DistributionSnapshot runtimeSnapshot = null;

    protected static Map<String, DistributionSnapshot> persistentSnapshots = new HashMap<String, DistributionSnapshot>();

    public static final String RUNTIME="current";

    public static DistributionSnapshot getRuntimeSnapshot() {
        if (runtimeSnapshot==null) {
            runtimeSnapshot = new RuntimeSnapshot();
        }
        return runtimeSnapshot;
    }

    public static void addPersistentSnapshot(String key, DistributionSnapshot snapshot) {
        persistentSnapshots.put(key, snapshot);
    }

    public static DistributionSnapshot getSnapshot(String key, CoreSession session) {
        if (key==null || RUNTIME.equals(key)) {
            return getRuntimeSnapshot();
        }
        readPersistentSnapshots(session);
        return persistentSnapshots.get(key);
    }

    public static List<DistributionSnapshot> readPersistentSnapshots(CoreSession session) {
        List<DistributionSnapshot> snaps = RepositoryDistributionSnapshot.readPersistentSnapshots(session);

        for (DistributionSnapshot snap : snaps) {
            addPersistentSnapshot(snap.getKey(), snap);
        }
        return snaps;
    }

    public static Map<String, DistributionSnapshot> getPersistentSnapshots(CoreSession session) {
        if (persistentSnapshots==null || persistentSnapshots.size()==0) {
            if (session!=null) {
                readPersistentSnapshots(session);
            } else {
                persistentSnapshots = new HashMap<String, DistributionSnapshot>();
            }
        }
        return persistentSnapshots;
    }

    public static List<String> getPersistentSnapshotNames(CoreSession session) {
        List<String> names = new ArrayList<String>();
        names.addAll(getPersistentSnapshots(session).keySet());
        return names;
    }

    public static List<String> getAvailableDistributions(CoreSession session) {
        List<String> names = new ArrayList<String>();
        names.addAll(getPersistentSnapshots(session).keySet());
        names.add(0,RUNTIME);
        return names;
    }

    public static List<String> getAvailableVersions(CoreSession session, NuxeoArtifact nxItem) {
        List<String> versions = new ArrayList<String>();

        Map<String, DistributionSnapshot> distribs = getPersistentSnapshots(session);

        DistributionSnapshot runtime = getRuntimeSnapshot();
        if (!distribs.containsKey(runtime.getKey())) {
            distribs.put(runtime.getKey(), runtime);
        }

        for (DistributionSnapshot snap : distribs.values()) {

            String version = null;
            if (BundleGroup.TYPE_NAME.equals(nxItem.getArtifactType())) {
                BundleGroup bg = snap.getBundleGroup(nxItem.getId());
                if (bg!=null) {
                    version = bg.getVersion();
                }
            }
            else if (BundleInfo.TYPE_NAME.equals(nxItem.getArtifactType())) {
                BundleInfo bi = snap.getBundle(nxItem.getId());
                if (bi!=null) {
                    version = bi.getVersion();
                }
            }
            else if (ComponentInfo.TYPE_NAME.equals(nxItem.getArtifactType())) {
                ComponentInfo ci = snap.getComponent(nxItem.getId());
                if (ci!=null) {
                    version = ci.getVersion();
                }
            }
            else if (ExtensionInfo.TYPE_NAME.equals(nxItem.getArtifactType())) {
                ExtensionInfo ei = snap.getContribution(nxItem.getId());
                if (ei!=null) {
                    version = ei.getVersion();
                }
            }
            else if (ExtensionPointInfo.TYPE_NAME.equals(nxItem.getArtifactType())) {
                ExtensionPointInfo epi = snap.getExtensionPoint(nxItem.getId());
                if (epi!=null) {
                    version = epi.getVersion();
                }
            }
            else if (ServiceInfo.TYPE_NAME.equals(nxItem.getArtifactType())) {
                ServiceInfo si = snap.getService(nxItem.getId());
                if (si!=null) {
                    version = si.getVersion();
                }
            }

            if (version!=null && !versions.contains(version)) {
                versions.add(version);
            }
        }
        return versions;
    }
}
