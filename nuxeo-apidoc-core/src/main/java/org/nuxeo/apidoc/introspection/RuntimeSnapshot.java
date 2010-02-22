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

package org.nuxeo.apidoc.introspection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleGroupFlatTree;
import org.nuxeo.apidoc.api.BundleGroupTreeHelper;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.repository.SnapshotPersister;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */
public class RuntimeSnapshot extends BaseNuxeoArtifact implements DistributionSnapshot {

    protected ServerInfo serverInfo;
    protected List<String> bundlesIds = new ArrayList<String>();
    protected Map<String, String> components2Bundles = new HashMap<String, String>();
    protected Map<String, String> services2Components = new HashMap<String, String>();
    protected Map<String, ExtensionPointInfo> extensionPoints = new HashMap<String, ExtensionPointInfo>();
    protected Map<String, ExtensionInfo> contributions = new HashMap<String, ExtensionInfo>();
    protected Map<String, List<String>> mavenGroups = new HashMap<String, List<String>>();
    protected Map<String, List<String>> mavenSubGroups = new HashMap<String, List<String>>();
    protected List<BundleGroup> bundleGroups = new ArrayList<BundleGroup>();

    protected List<Class> spi = new ArrayList<Class>();

    public RuntimeSnapshot() {
        buildServerInfo();
    }

    public String getVersion() {
        return serverInfo.getVersion();
    }

    public String getName() {
        return serverInfo.getName();
    }


    protected synchronized ServerInfo buildServerInfo() {
        if (serverInfo == null) {
            serverInfo = ServerInfo.build();
            spi.addAll(serverInfo.getAllSpi());
            for (BundleInfoImpl bInfo : serverInfo.getBundles()) {
                bundlesIds.add(bInfo.getBundleId());

                String groupId = bInfo.getArtifactGroupId();
                String artifactId = bInfo.getArtifactId();

                if (groupId != null && artifactId != null) {
                    if (!mavenGroups.containsKey(groupId)) {
                        mavenGroups.put(groupId, new ArrayList<String>());
                    }
                    mavenGroups.get(groupId).add(bInfo.getBundleId());
                }

                for (ComponentInfo cInfo : bInfo.getComponents()) {
                    components2Bundles
                            .put(cInfo.getName(), bInfo.getBundleId());

                    for (String serviceName : cInfo.getServiceNames()) {
                        services2Components.put(serviceName, cInfo.getName());
                    }

                    for (ExtensionPointInfo epi : cInfo.getExtensionPoints()) {
                        extensionPoints.put(epi.getName(), epi);
                    }

                    for (ExtensionInfo ei : cInfo.getExtensions()) {
                        contributions.put(ei.getId(), ei);
                    }
                }
            }
        }

        // post process mavenGroups
        List<String> mvnGroupNames = new ArrayList<String>();
        mvnGroupNames.addAll(mavenGroups.keySet());

        for (String mvnGroupName : mvnGroupNames) {
            List<String> artifactIds = mavenGroups.get(mvnGroupName);
            Collections.sort(artifactIds);

            List<String> subGroups = new ArrayList<String>();

            for (String id : artifactIds) {
                if (id.endsWith(".api")) {
                    String grp = id.substring(0, id.length()-4);

                    if (grp.equals(mvnGroupName)) {
                        continue;
                    }

                    subGroups.add(grp);
                }
            }

            for (String grp : subGroups) {
                List<String> grpArtifactIds = new ArrayList<String>();
                for (String aid : artifactIds) {
                    if (aid.startsWith(grp)) {
                       grpArtifactIds.add(aid);
                    }
                }
                if (grpArtifactIds.size()>1) {
                    for (String aid:grpArtifactIds) {
                        artifactIds.remove(aid);
                    }
                    mavenSubGroups.put(grp,grpArtifactIds);
                    artifactIds.add("grp:" + grp);
                }
            }
            //mavenGroups.put(mvnGroupName, artifactIds);
        }

        for (String grpId : mavenGroups.keySet()) {
            BundleGroupImpl bGroup = buildBundleGroup(grpId, serverInfo.getVersion());
            bundleGroups.add(bGroup);
        }
        return serverInfo;

    }

    protected BundleGroupImpl buildBundleGroup(String id, String version) {
        BundleGroupImpl bGroup = new BundleGroupImpl(id, version);
        for (String aid : getBundleGroupChildren(id)) {
            if (aid.startsWith("grp:")) {
                bGroup.add(buildBundleGroup(aid, version));
            } else {
                bGroup.add(aid);
            }
        }
        return bGroup;
    }

    public List<BundleGroup> getBundleGroups() {
        return bundleGroups;
    }

    public BundleGroup getBundleGroup(String groupId) {

        BundleGroupTreeHelper bgth = new BundleGroupTreeHelper(this);
        List<BundleGroupFlatTree> tree = bgth.getBundleGroupTree();

        for (BundleGroupFlatTree info : tree) {

            if (info.getGroup().getName().equals(groupId) || info.getGroup().getKey().equals(groupId)) {
                return info.getGroup();
            }
        }
        return null;
    }

    protected void browseBundleGroup(BundleGroup group, int level, List<BundleGroupFlatTree> tree) {

        BundleGroupFlatTree info = new BundleGroupFlatTree(group, level);
        tree.add(info);

        for (BundleGroup subGroup : group.getSubGroups()) {
            browseBundleGroup(subGroup, level+1, tree);
        }
    }

    public List<String> getBundleIds() {
        List<String> bundlesIds = new ArrayList<String>();

        for (BundleInfoImpl info : serverInfo.getBundles()) {
            bundlesIds.add(info.getBundleId());

        }
        Collections.sort(bundlesIds);
        return bundlesIds;
    }

    public BundleInfoImpl getBundle(String id) {
        return serverInfo.getBundle(id);
    }

    public List<String> getComponentIds() {
        List<String> componentsIds = new ArrayList<String>();
        componentsIds.addAll(components2Bundles.keySet());
        Collections.sort(componentsIds);
        return componentsIds;
    }

    public ComponentInfo getComponent(String id) {
        String bundleId = components2Bundles.get(id);
        BundleInfoImpl bi = getBundle(bundleId);

        for (ComponentInfo ci : bi.getComponents()) {
            if (ci.getName().equals(id)) {
                return ci;
            }
        }
        return null;
    }

    public List<String> getServiceIds() {
        List<String> serviceIds = new ArrayList<String>();
        serviceIds.addAll(services2Components.keySet());
        Collections.sort(serviceIds);
        return serviceIds;
    }

    public List<String> getExtensionPointIds() {
        List<String> epIds = new ArrayList<String>();
        epIds.addAll(extensionPoints.keySet());
        Collections.sort(epIds);
        return epIds;
    }

    public ExtensionPointInfo getExtensionPoint(String id) {
        return extensionPoints.get(id);
    }

    public List<String> getContributionIds() {
        List<String> contribIds = new ArrayList<String>();
        contribIds.addAll(contributions.keySet());
        Collections.sort(contribIds);
        return contribIds;
    }

    public ExtensionInfo getContribution(String id) {
        return contributions.get(id);
    }

    public List<String> getBundleGroupIds() {
        List<String> grpIds = new ArrayList<String>();
        grpIds.addAll(mavenGroups.keySet());
        Collections.sort(grpIds);
        return grpIds;
    }

    public List<String> getBundleGroupChildren(String groupId) {

        List<String> res = null;
        if (groupId.startsWith("grp:")) {
            String grpId = groupId.substring(4);
            res = mavenSubGroups.get(grpId);
        } else {
            res = mavenGroups.get(groupId);
        }

        if (res!=null) {
            return res;
        } else {
            return new ArrayList<String>();
        }
    }

    public String getKey() {
        return getName() + "-" + getVersion();
    }

    public DistributionSnapshot persist(CoreSession session) throws ClientException {
        SnapshotPersister sp = new SnapshotPersister();
        DistributionSnapshot snap =  sp.persist(this, session, this.getKey());
        SnapshotManager.addPersistentSnapshot(snap.getKey(), snap);
        return snap;
    }

    public List<Class> getSpi() {
        return spi;
    }

    @Override
    public String getId() {
        return getKey();
    }

    public String getArtifactType() {
        return DistributionSnapshot.TYPE_NAME;
    }

}
