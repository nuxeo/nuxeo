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
package org.nuxeo.apidoc.introspection;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleGroupFlatTree;
import org.nuxeo.apidoc.api.BundleGroupTreeHelper;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.SeamComponentInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.api.graph.Graph;
import org.nuxeo.apidoc.documentation.JavaDocHelper;
import org.nuxeo.apidoc.introspection.graph.BasicGraphGeneratorImpl;
import org.nuxeo.apidoc.introspection.graph.GephiGraphGeneratorImpl;
import org.nuxeo.apidoc.introspection.graph.JGraphGeneratorImpl;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RuntimeSnapshot extends BaseNuxeoArtifact implements DistributionSnapshot {

    public static final String VIRTUAL_BUNDLE_GROUP = "grp:org.nuxeo.misc";

    protected ServerInfo serverInfo;

    protected Date created;

    protected final List<String> bundleIds = new ArrayList<>();

    protected final List<String> javaComponentsIds = new ArrayList<>();

    protected final Map<String, String> components2Bundles = new HashMap<>();

    protected final Map<String, String> services2Components = new HashMap<>();

    protected final Map<String, ExtensionPointInfo> extensionPoints = new HashMap<>();

    protected final Map<String, ExtensionInfo> contributions = new HashMap<>();

    protected final Map<String, List<String>> mavenGroups = new HashMap<>();

    protected final Map<String, List<String>> mavenSubGroups = new HashMap<>();

    protected final List<BundleGroup> bundleGroups = new ArrayList<>();

    protected boolean seamInitialized = false;

    protected List<SeamComponentInfo> seamComponents = new ArrayList<>();

    protected boolean opsInitialized = false;

    protected final List<OperationInfo> operations = new ArrayList<>();

    protected JavaDocHelper jdocHelper;

    protected final List<Class<?>> spi = new ArrayList<>();

    protected final List<String> aliases = new LinkedList<>(Collections.singletonList("current"));

    protected final List<Graph> graphs = new ArrayList<>();

    public static RuntimeSnapshot build() {
        return new RuntimeSnapshot();
    }

    @JsonCreator
    private RuntimeSnapshot(@JsonProperty("serverInfo") ServerInfo serverInfo,
            @JsonProperty("creationDate") Date created,
            @JsonProperty("seamComponents") List<SeamComponentInfo> seamComponents,
            @JsonProperty("operations") List<OperationInfo> operations) {
        this.serverInfo = serverInfo;
        this.created = created;
        index();
        this.seamComponents.addAll(seamComponents);
        this.operations.addAll(operations);
    }

    protected RuntimeSnapshot() {
        serverInfo = ServerInfo.build();
        created = new Date();

        index();
    }

    @Override
    public ServerInfo getServerInfo() {
        return serverInfo;
    }

    @Override
    @JsonIgnore
    public String getVersion() {
        return serverInfo.getVersion();
    }

    @Override
    @JsonIgnore
    public String getName() {
        return serverInfo.getName();
    }

    protected void index() {
        spi.addAll(serverInfo.getAllSpi());
        for (BundleInfo bInfo : serverInfo.getBundles()) {
            bundleIds.add(bInfo.getId());

            String groupId = bInfo.getGroupId();
            if (groupId != null) {
                groupId = "grp:" + groupId;
            }
            String artifactId = bInfo.getArtifactId();

            if (groupId == null || artifactId == null) {
                groupId = VIRTUAL_BUNDLE_GROUP;
                ((BundleInfoImpl) bInfo).setGroupId(groupId);
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

                for (ServiceInfo sInfo : cInfo.getServices()) {
                    if (sInfo.isOverriden()) {
                        continue;
                    }
                    services2Components.put(sInfo.getId(), cInfo.getId());
                }

                for (ExtensionPointInfo epi : cInfo.getExtensionPoints()) {
                    extensionPoints.put(epi.getId(), epi);
                }

                Map<String, Integer> comps = new HashMap<String, Integer>();
                for (ExtensionInfo ei : cInfo.getExtensions()) {
                    // handle multiple contributions to the same extension point
                    String id = ei.getId();
                    if (comps.containsKey(id)) {
                        Integer num = comps.get(id);
                        comps.put(id, num + 1);
                        id += "-" + String.valueOf(num + 1);
                    } else {
                        comps.put(id, Integer.valueOf(0));
                    }
                    contributions.put(id, ei);
                }
            }
        }
        // post process bundle groups
        List<String> mvnGroupNames = new ArrayList<>();
        mvnGroupNames.addAll(mavenGroups.keySet());

        for (String mvnGroupName : mvnGroupNames) {
            List<String> artifactIds = mavenGroups.get(mvnGroupName);
            Collections.sort(artifactIds);

            List<String> subGroups = new ArrayList<>();

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
                    List<String> grpArtifactIds = new ArrayList<>();
                    for (String aid : artifactIds) {
                        if (aid.startsWith(grp) || ("grp:" + aid).startsWith(grp)) {
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
            BundleGroupImpl bGroup = buildBundleGroup(grpId, serverInfo.getVersion());
            bundleGroups.add(bGroup);
        }

        graphs.add(BasicGraphGeneratorImpl.getGraph("basic", this));
        graphs.add(JGraphGeneratorImpl.getGraph("jgrapht", this));
        graphs.add(GephiGraphGeneratorImpl.getGraph("gephi", this));
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
                ((BundleInfoImpl) getBundle(aid)).setBundleGroup(bGroup);
                BundleInfo bi = getBundle(aid);
                bGroup.addLiveDoc(bi.getParentLiveDoc());
            }
        }
        return bGroup;
    }

    @Override
    @JsonIgnore
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

    protected void browseBundleGroup(BundleGroup group, int level, List<BundleGroupFlatTree> tree) {
        BundleGroupFlatTree info = new BundleGroupFlatTree(group, level);
        tree.add(info);

        for (BundleGroup subGroup : group.getSubGroups()) {
            browseBundleGroup(subGroup, level + 1, tree);
        }
    }

    @Override
    @JsonIgnore
    public List<String> getBundleIds() {
        List<String> bundlesIds = new ArrayList<>();

        for (BundleInfo info : serverInfo.getBundles()) {
            bundlesIds.add(info.getId());

        }
        Collections.sort(bundlesIds);
        return bundlesIds;
    }

    @Override
    public BundleInfo getBundle(String id) {
        return serverInfo.getBundle(id);
    }

    @Override
    @JsonIgnore
    public List<String> getComponentIds() {
        List<String> componentsIds = new ArrayList<>();
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
        BundleInfo bi = getBundle(bundleId);

        for (ComponentInfo ci : bi.getComponents()) {
            if (ci.getId().equals(id)) {
                return ci;
            }
        }
        return null;
    }

    @Override
    @JsonIgnore
    public List<String> getServiceIds() {
        List<String> serviceIds = new ArrayList<>();
        serviceIds.addAll(services2Components.keySet());
        Collections.sort(serviceIds);
        return serviceIds;
    }

    @Override
    @JsonIgnore
    public List<String> getExtensionPointIds() {
        List<String> epIds = new ArrayList<>();
        epIds.addAll(extensionPoints.keySet());
        Collections.sort(epIds);
        return epIds;
    }

    @Override
    @JsonIgnore
    public ExtensionPointInfo getExtensionPoint(String id) {
        return extensionPoints.get(id);
    }

    @Override
    @JsonIgnore
    public List<String> getContributionIds() {
        List<String> contribIds = new ArrayList<>();
        contribIds.addAll(contributions.keySet());
        Collections.sort(contribIds);
        return contribIds;
    }

    @Override
    @JsonIgnore
    public List<ExtensionInfo> getContributions() {
        List<ExtensionInfo> contribs = new ArrayList<>();
        contribs.addAll(contributions.values());
        return contribs;
    }

    @Override
    public ExtensionInfo getContribution(String id) {
        return contributions.get(id);
    }

    public List<String> getBundleGroupIds() {
        List<String> grpIds = new ArrayList<>();
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
            return new ArrayList<>();
        }
    }

    @Override
    @JsonIgnore
    public String getKey() {
        return getName() + "-" + getVersion();
    }

    @Override
    @JsonIgnore
    public List<Class<?>> getSpi() {
        return spi;
    }

    @Override
    @JsonIgnore
    public String getId() {
        return getKey();
    }

    @Override
    @JsonIgnore
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
    @JsonIgnore
    public List<String> getJavaComponentIds() {
        return javaComponentsIds;
    }

    @Override
    @JsonIgnore
    public List<String> getXmlComponentIds() {
        List<String> result = new ArrayList<>();

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
    @JsonIgnore
    public Date getReleaseDate() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isLive() {
        return true;
    }

    @Override
    @JsonIgnore
    public String getHierarchyPath() {
        return null;
    }

    @SuppressWarnings("unchecked")
    public void initSeamComponents(HttpServletRequest request) {
        if (seamInitialized) {
            return;
        }
        // use reflection to call SeamRuntimeIntrospector, if available
        try {
            // SeamRuntimeIntrospector.listNuxeoComponents(request);
            Class<?> klass = Class.forName("org.nuxeo.apidoc.seam.SeamRuntimeIntrospector");
            Method method = klass.getDeclaredMethod("listNuxeoComponents", HttpServletRequest.class);
            seamComponents = (List<SeamComponentInfo>) method.invoke(null, request);
        } catch (ReflectiveOperationException e) {
            // ignore, no Seam
        }
        for (SeamComponentInfo seamComp : seamComponents) {
            ((SeamComponentInfoImpl) seamComp).setVersion(getVersion());
        }
        seamInitialized = true;
    }

    public void initOperations() {
        if (opsInitialized) {
            return;
        }
        AutomationService service = Framework.getService(AutomationService.class);
        if (service == null) {
            return;
        }
        OperationType[] ops = service.getOperations();
        for (OperationType op : ops) {
            OperationDocumentation documentation;
            try {
                documentation = op.getDocumentation();
            } catch (OperationException e) {
                throw new NuxeoException(e);
            }
            operations.add(new OperationInfoImpl(documentation, getVersion(), op.getType().getCanonicalName(),
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
    @JsonIgnore
    public List<String> getSeamComponentIds() {
        List<String> ids = new ArrayList<>();
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
        return getSeamComponentIds().size() > 0;
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

            String finalId = id;
            Optional<String> first = Arrays.stream(op.getAliases()).filter(s -> s.equals(finalId)).findFirst();
            if (first.isPresent()) {
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

    public JavaDocHelper getJavaDocHelper() {
        if (jdocHelper == null) {
            jdocHelper = JavaDocHelper.getHelper(getName(), getVersion());
        }
        return jdocHelper;
    }

    @Override
    public void cleanPreviousArtifacts() {
        // Can't delete anything in a runtime Snapshot
        throw new UnsupportedOperationException();
    }

    @Override
    @JsonIgnore
    public boolean isLatestFT() {
        return false;
    }

    @Override
    @JsonIgnore
    public boolean isLatestLTS() {
        return false;
    }

    @Override
    @JsonIgnore
    public List<String> getAliases() {
        return aliases;
    }

    @Override
    @JsonIgnore
    public boolean isHidden() {
        return false;
    }

    @Override
    public List<Graph> getGraphs() {
        return graphs;
    }

}
