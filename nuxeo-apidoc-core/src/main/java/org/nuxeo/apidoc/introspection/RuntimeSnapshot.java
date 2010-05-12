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
import java.util.Date;
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
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.repository.SnapshotPersister;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */
public class RuntimeSnapshot extends BaseNuxeoArtifact implements DistributionSnapshot {

    protected ServerInfo serverInfo;
    protected Date created = null;
    protected List<String> bundlesIds = new ArrayList<String>();
    protected List<String> javaComponentsIds = new ArrayList<String>();
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
            created=new Date();
            spi.addAll(serverInfo.getAllSpi());
            for (BundleInfoImpl bInfo : serverInfo.getBundles()) {
                bundlesIds.add(bInfo.getId());

                String groupId = "grp:" + bInfo.getArtifactGroupId();
                String artifactId = bInfo.getArtifactId();

                if (groupId != null && artifactId != null) {
                    if (!mavenGroups.containsKey(groupId)) {
                        mavenGroups.put(groupId, new ArrayList<String>());
                    }
                    mavenGroups.get(groupId).add(bInfo.getId());
                }

                for (ComponentInfo cInfo : bInfo.getComponents()) {
                    components2Bundles
                            .put(cInfo.getId(), bInfo.getId());
                    if (!cInfo.isXmlPureComponent()) {
                        javaComponentsIds.add(cInfo.getId());
                    }

                    for (String serviceName : cInfo.getServiceNames()) {
                        services2Components.put(serviceName, cInfo.getId());
                    }

                    for (ExtensionPointInfo epi : cInfo.getExtensionPoints()) {
                        extensionPoints.put(epi.getId(), epi);
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
                    String grp = "grp:" + id.substring(0, id.length()-4);

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
                    } else if (("grp:" + aid).startsWith(grp)){
                       grpArtifactIds.add(aid);
                    }
                }
                if (grpArtifactIds.size()>0) {
                    for (String aid:grpArtifactIds) {
                        artifactIds.remove(aid);
                    }
                    mavenSubGroups.put(grp,grpArtifactIds);
                    artifactIds.add( grp);
                }
            }
            //mavenGroups.put(mvnGroupName, artifactIds);
        }

        for (String grpId : mavenGroups.keySet()) {
            if (!grpId.startsWith("grp:")) {
                grpId = "grp:" + grpId;
            }
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

            if (info.getGroup().getId().equals(groupId) || info.getGroup().getId().equals(groupId)) {
                return info.getGroup();
            }
        }
        if (!groupId.startsWith("grp:")) {
            return getBundleGroup("grp:" + groupId);
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
            bundlesIds.add(info.getId());

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
            if (ci.getId().equals(id)) {
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
        res = mavenSubGroups.get(groupId);
        if (res==null) {
            //String grpId = groupId.substring(4);
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

    public ServiceInfo getService(String id) {
        String cId = services2Components.get(id);
        if (cId == null) {
            return null;
        }

        for (ServiceInfo si : getComponent(cId).getServices()) {
            if (id.equals(id)) {
                return si;
            }
        }
        return null;
    }

    public List<String> getJavaComponentIds() {
        return javaComponentsIds;
    }

    public List<String> getXmlComponentIds() {
        List<String> result = new ArrayList<String>();

        for (String cId : getComponentIds()) {
            if (!javaComponentsIds.contains(cId)) {
                result.add(cId);
            }
        }
        return result;
    }

    public Date getCreationDate() {
        return created;
    }

    public boolean isLive() {
        return true;
    }

}
