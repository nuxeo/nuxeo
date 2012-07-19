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
package org.nuxeo.apidoc.introspection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleGroupFlatTree;
import org.nuxeo.apidoc.api.BundleGroupTreeHelper;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.SeamComponentInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.documentation.JavaDocHelper;
import org.nuxeo.apidoc.seam.SeamRuntimeIntrospector;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.runtime.api.Framework;

public class RuntimeSnapshot extends BaseNuxeoArtifact implements
        DistributionSnapshot {

    public static final String VIRTUAL_BUNDLE_GROUP = "grp:org.nuxeo.misc";

    protected ServerInfo serverInfo;

    protected Date created;

    protected final List<String> bundlesIds = new ArrayList<String>();

    protected final List<String> javaComponentsIds = new ArrayList<String>();

    protected final Map<String, String> components2Bundles = new HashMap<String, String>();

    protected final Map<String, String> services2Components = new HashMap<String, String>();

    protected final Map<String, ExtensionPointInfo> extensionPoints = new HashMap<String, ExtensionPointInfo>();

    protected final Map<String, ExtensionInfo> contributions = new HashMap<String, ExtensionInfo>();

    protected final Map<String, List<String>> mavenGroups = new HashMap<String, List<String>>();

    protected final Map<String, List<String>> mavenSubGroups = new HashMap<String, List<String>>();

    protected final List<BundleGroup> bundleGroups = new ArrayList<BundleGroup>();

    protected boolean seamInitialized = false;

    protected List<SeamComponentInfo> seamComponents = new ArrayList<SeamComponentInfo>();

    protected boolean opsInitialized = false;

    protected final List<OperationInfo> operations = new ArrayList<OperationInfo>();

    protected JavaDocHelper jdocHelper;

    protected final List<Class<?>> spi = new ArrayList<Class<?>>();

    public RuntimeSnapshot() {
        buildServerInfo();
    }

    @Override
    public String getVersion() {
        return serverInfo.getVersion();
    }

    @Override
    public String getName() {
        return serverInfo.getName();
    }

    protected synchronized ServerInfo buildServerInfo() {
        if (serverInfo == null) {
            serverInfo = ServerInfo.build();
            created = new Date();
            spi.addAll(serverInfo.getAllSpi());
            for (BundleInfoImpl bInfo : serverInfo.getBundles()) {
                bundlesIds.add(bInfo.getId());

                String groupId = bInfo.getArtifactGroupId();
                if (groupId != null) {
                    groupId = "grp:" + groupId;
                }
                String artifactId = bInfo.getArtifactId();

                if (groupId == null || artifactId == null) {
                    groupId = VIRTUAL_BUNDLE_GROUP;
                    bInfo.setGroupId(groupId);
                }
                if (!mavenGroups.containsKey(groupId)) {
                    mavenGroups.put(groupId, new ArrayList<String>());
                }
                mavenGroups.get(groupId).add(bInfo.getId());

                for (ComponentInfo cInfo : bInfo.getComponents()) {
                    components2Bundles.put(cInfo.getId(), bInfo.getId());
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

        // post process bundle groups
        List<String> mvnGroupNames = new ArrayList<String>();
        mvnGroupNames.addAll(mavenGroups.keySet());

        for (String mvnGroupName : mvnGroupNames) {
            List<String> artifactIds = mavenGroups.get(mvnGroupName);
            Collections.sort(artifactIds);

            List<String> subGroups = new ArrayList<String>();

            for (String id : artifactIds) {
                if (id.endsWith(".api")) {
                    String grp = "grp:" + id.substring(0, id.length() - 4);

                    if (grp.equals(mvnGroupName)) {
                        continue;
                    }

                    subGroups.add(grp);
                }
            }

            if (subGroups.size() < 2) {
                // no need to split the maven group into subGroups
            } else {
                for (String grp : subGroups) {
                    List<String> grpArtifactIds = new ArrayList<String>();
                    for (String aid : artifactIds) {
                        if (aid.startsWith(grp)
                                || ("grp:" + aid).startsWith(grp)) {
                            grpArtifactIds.add(aid);
                        }
                    }
                    if (grpArtifactIds.size() > 0) {
                        for (String aid : grpArtifactIds) {
                            artifactIds.remove(aid);
                        }
                        mavenSubGroups.put(grp, grpArtifactIds);
                        artifactIds.add(grp);
                    }
                }
            }
        }

        for (String grpId : mavenGroups.keySet()) {
            BundleGroupImpl bGroup = buildBundleGroup(grpId,
                    serverInfo.getVersion());
            bundleGroups.add(bGroup);
        }
        return serverInfo;
    }

    protected BundleGroupImpl buildBundleGroup(String id, String version) {
        BundleGroupImpl bGroup = new BundleGroupImpl(id, version);
        for (String aid : getBundleGroupChildren(id)) {
            if (aid.startsWith("grp:")) {
                BundleGroupImpl newGroup = buildBundleGroup(aid, version);
                bGroup.add(newGroup);
                newGroup.addParent(bGroup.getId());
            } else {
                bGroup.add(aid);
                getBundle(aid).setBundleGroup(bGroup);
                BundleInfoImpl bi = getBundle(aid);
                bGroup.addLiveDoc(bi.getParentLiveDoc());
            }
        }
        return bGroup;
    }

    @Override
    public List<BundleGroup> getBundleGroups() {
        return bundleGroups;
    }

    @Override
    public BundleGroup getBundleGroup(String groupId) {
        BundleGroupTreeHelper bgth = new BundleGroupTreeHelper(this);
        List<BundleGroupFlatTree> tree = bgth.getBundleGroupTree();

        for (BundleGroupFlatTree info : tree) {
            if (info.getGroup().getId().equals(groupId)) {
                return info.getGroup();
            }
        }
        if (!groupId.startsWith("grp:")) {
            return getBundleGroup("grp:" + groupId);
        }
        return null;
    }

    protected void browseBundleGroup(BundleGroup group, int level,
            List<BundleGroupFlatTree> tree) {
        BundleGroupFlatTree info = new BundleGroupFlatTree(group, level);
        tree.add(info);

        for (BundleGroup subGroup : group.getSubGroups()) {
            browseBundleGroup(subGroup, level + 1, tree);
        }
    }

    @Override
    public List<String> getBundleIds() {
        List<String> bundlesIds = new ArrayList<String>();

        for (BundleInfoImpl info : serverInfo.getBundles()) {
            bundlesIds.add(info.getId());

        }
        Collections.sort(bundlesIds);
        return bundlesIds;
    }

    @Override
    public BundleInfoImpl getBundle(String id) {
        return serverInfo.getBundle(id);
    }

    @Override
    public List<String> getComponentIds() {
        List<String> componentsIds = new ArrayList<String>();
        componentsIds.addAll(components2Bundles.keySet());
        Collections.sort(componentsIds);
        return componentsIds;
    }

    @Override
    public ComponentInfo getComponent(String id) {
        String bundleId = components2Bundles.get(id);
        if (bundleId == null) {
            return null;
        }
        BundleInfoImpl bi = getBundle(bundleId);

        for (ComponentInfo ci : bi.getComponents()) {
            if (ci.getId().equals(id)) {
                return ci;
            }
        }
        return null;
    }

    @Override
    public List<String> getServiceIds() {
        List<String> serviceIds = new ArrayList<String>();
        serviceIds.addAll(services2Components.keySet());
        Collections.sort(serviceIds);
        return serviceIds;
    }

    @Override
    public List<String> getExtensionPointIds() {
        List<String> epIds = new ArrayList<String>();
        epIds.addAll(extensionPoints.keySet());
        Collections.sort(epIds);
        return epIds;
    }

    @Override
    public ExtensionPointInfo getExtensionPoint(String id) {
        return extensionPoints.get(id);
    }

    @Override
    public List<String> getContributionIds() {
        List<String> contribIds = new ArrayList<String>();
        contribIds.addAll(contributions.keySet());
        Collections.sort(contribIds);
        return contribIds;
    }

    @Override
    public List<ExtensionInfo> getContributions() {
        List<ExtensionInfo> contribs = new ArrayList<ExtensionInfo>();
        contribs.addAll(contributions.values());
        // TODO sort
        return contribs;
    }

    @Override
    public ExtensionInfo getContribution(String id) {
        return contributions.get(id);
    }

    public List<String> getBundleGroupIds() {
        List<String> grpIds = new ArrayList<String>();
        grpIds.addAll(mavenGroups.keySet());
        Collections.sort(grpIds);
        return grpIds;
    }

    @Override
    public List<String> getBundleGroupChildren(String groupId) {
        List<String> res = mavenSubGroups.get(groupId);
        if (res == null) {
            // String grpId = groupId.substring(4);
            res = mavenGroups.get(groupId);
        }

        if (res != null) {
            return res;
        } else {
            return new ArrayList<String>();
        }
    }

    @Override
    public String getKey() {
        return getName() + "-" + getVersion();
    }

    @Override
    public List<Class<?>> getSpi() {
        return spi;
    }

    @Override
    public String getId() {
        return getKey();
    }

    @Override
    public String getArtifactType() {
        return TYPE_NAME;
    }

    @Override
    public ServiceInfo getService(String id) {
        String cId = services2Components.get(id);
        if (cId == null) {
            return null;
        }

        for (ServiceInfo si : getComponent(cId).getServices()) {
            if (id.equals(si.getId())) {
                return si;
            }
        }
        return null;
    }

    @Override
    public List<String> getJavaComponentIds() {
        return javaComponentsIds;
    }

    @Override
    public List<String> getXmlComponentIds() {
        List<String> result = new ArrayList<String>();

        for (String cId : getComponentIds()) {
            if (!javaComponentsIds.contains(cId)) {
                result.add(cId);
            }
        }
        return result;
    }

    @Override
    public Date getCreationDate() {
        return created;
    }

    @Override
    public boolean isLive() {
        return true;
    }

    @Override
    public String getHierarchyPath() {
        // TODO Auto-generated method stub
        return null;
    }

    public void initSeamComponents(HttpServletRequest request) {
        if (seamInitialized) {
            return;
        }
        seamComponents = SeamRuntimeIntrospector.listNuxeoComponents(request);
        for (SeamComponentInfo seamComp : seamComponents) {
            ((SeamComponentInfoImpl) seamComp).setVersion(getVersion());
        }
        seamInitialized = true;
    }

    public void initOperations() {
        if (opsInitialized) {
            return;
        }
        OperationType[] ops;
        try {
            ops = Framework.getService(AutomationService.class).getOperations();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        for (OperationType op : ops) {
            operations.add(new OperationInfoImpl(op.getDocumentation(),
                    getVersion(), op.getType().getCanonicalName(),
                    op.getContributingComponent()));
        }
        opsInitialized = true;
    }

    @Override
    public SeamComponentInfo getSeamComponent(String id) {
        for (SeamComponentInfo sci : getSeamComponents()) {
            if (sci.getId().equals(id)) {
                return sci;
            }
        }
        return null;
    }

    @Override
    public List<String> getSeamComponentIds() {
        List<String> ids = new ArrayList<String>();
        for (SeamComponentInfo sci : getSeamComponents()) {
            ids.add(sci.getId());
        }
        return ids;
    }

    @Override
    public List<SeamComponentInfo> getSeamComponents() {
        return seamComponents;
    }

    @Override
    public boolean containsSeamComponents() {
        return seamInitialized && getSeamComponentIds().size() > 0;
    }

    @Override
    public OperationInfo getOperation(String id) {
        if (id.startsWith(OperationInfo.ARTIFACT_PREFIX)) {
            id = id.substring(OperationInfo.ARTIFACT_PREFIX.length());
        }
        for (OperationInfo op : getOperations()) {
            if (op.getName().equals(id)) {
                return op;
            }
        }
        return null;
    }

    @Override
    public List<OperationInfo> getOperations() {
        initOperations();
        return operations;
    }

    @Override
    public JavaDocHelper getJavaDocHelper() {
        if (jdocHelper == null) {
            jdocHelper = JavaDocHelper.getHelper(getName(), getVersion());
        }
        return jdocHelper;
    }

}
