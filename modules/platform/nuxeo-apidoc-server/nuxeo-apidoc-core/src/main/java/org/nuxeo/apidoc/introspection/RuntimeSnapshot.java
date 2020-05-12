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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleGroupFlatTree;
import org.nuxeo.apidoc.api.BundleGroupTreeHelper;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.documentation.JavaDocHelper;
import org.nuxeo.apidoc.plugin.Plugin;
import org.nuxeo.apidoc.plugin.PluginSnapshot;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

public class RuntimeSnapshot extends BaseNuxeoArtifact implements DistributionSnapshot {

    public static final String VIRTUAL_BUNDLE_GROUP = "grp:org.nuxeo.misc";

    protected final Date created;

    protected final Date released;

    protected final String name;

    protected final String version;

    protected final Map<String, BundleInfo> bundles = new LinkedHashMap<>();

    protected final List<String> javaComponentsIds = new ArrayList<>();

    protected final Map<String, String> components2Bundles = new HashMap<>();

    protected final Map<String, String> services2Components = new HashMap<>();

    protected final Map<String, ExtensionPointInfo> extensionPoints = new LinkedHashMap<>();

    protected final Map<String, ExtensionInfo> contributions = new LinkedHashMap<>();

    protected final Map<String, List<String>> mavenGroups = new HashMap<>();

    protected final Map<String, List<String>> mavenSubGroups = new HashMap<>();

    protected final List<BundleGroup> bundleGroups = new ArrayList<>();

    protected boolean opsInitialized = false;

    protected final List<OperationInfo> operations = new ArrayList<>();

    protected JavaDocHelper jdocHelper;

    protected boolean pluginSnapshotsInitialized = false;

    protected final Map<String, PluginSnapshot<?>> pluginSnapshots = new HashMap<>();

    protected final List<String> aliases = new LinkedList<>(Collections.singletonList("current"));

    public static RuntimeSnapshot build() {
        return new RuntimeSnapshot();
    }

    @JsonCreator
    public RuntimeSnapshot(@JsonProperty("name") String name, @JsonProperty("version") String version,
            @JsonProperty("creationDate") Date created, @JsonProperty("releaseDate") Date released,
            @JsonProperty("bundles") List<BundleInfo> bundles,
            @JsonProperty("operations") List<OperationInfo> operations,
            @JsonProperty("pluginSnapshots") Map<String, PluginSnapshot<?>> pluginSnapshots) {
        this.created = created;
        this.released = released;
        this.name = name;
        this.version = version;
        index(bundles);
        if (operations != null) {
            this.operations.addAll(operations);
        }
        this.opsInitialized = true;
        if (pluginSnapshots != null) {
            this.pluginSnapshots.putAll(pluginSnapshots);
        }
        this.pluginSnapshotsInitialized = true;
    }

    protected RuntimeSnapshot() {
        created = new Date();
        released = null;
        ServerInfo serverInfo = ServerInfo.build();
        this.name = serverInfo.getName();
        this.version = serverInfo.getVersion();
        index(new ArrayList<>(serverInfo.getBundles()));
        initOperations();
        initPluginSnapshots();
    }

    protected void index(List<BundleInfo> distributionBundles) {
        if (distributionBundles == null) {
            return;
        }
        for (BundleInfo bInfo : distributionBundles) {
            bundles.put(bInfo.getId(), bInfo);

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

                Map<String, AtomicInteger> comps = new HashMap<>();
                for (ExtensionInfo ei : cInfo.getExtensions()) {
                    // handle multiple contributions to the same extension point
                    String id = ei.getId();
                    if (comps.containsKey(id)) {
                        int num = comps.get(id).incrementAndGet();
                        id += "-" + num;
                    } else {
                        comps.put(id, new AtomicInteger());
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
            BundleGroupImpl bGroup = buildBundleGroup(grpId, version);
            bundleGroups.add(bGroup);
        }

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
                BundleInfo bi = getBundle(aid);
                if (bi instanceof BundleInfoImpl) {
                    ((BundleInfoImpl) bi).setBundleGroup(bGroup);
                }
            }
        }
        return bGroup;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getName() {
        return name;
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

    protected void browseBundleGroup(BundleGroup group, int level, List<BundleGroupFlatTree> tree) {
        BundleGroupFlatTree info = new BundleGroupFlatTree(group, level);
        tree.add(info);

        for (BundleGroup subGroup : group.getSubGroups()) {
            browseBundleGroup(subGroup, level + 1, tree);
        }
    }

    @Override
    public List<String> getBundleIds() {
        return new ArrayList<>(bundles.keySet());
    }

    @Override
    public BundleInfo getBundle(String id) {
        return bundles.get(id);
    }

    @Override
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
    public List<String> getServiceIds() {
        List<String> serviceIds = new ArrayList<>();
        serviceIds.addAll(services2Components.keySet());
        Collections.sort(serviceIds);
        return serviceIds;
    }

    @Override
    public List<String> getExtensionPointIds() {
        List<String> epIds = new ArrayList<>();
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
        List<String> contribIds = new ArrayList<>();
        contribIds.addAll(contributions.keySet());
        Collections.sort(contribIds);
        return contribIds;
    }

    @Override
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
    public String getKey() {
        return getName() + "-" + getVersion();
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
    public Date getReleaseDate() {
        return released;
    }

    @Override
    public boolean isLive() {
        return true;
    }

    @Override
    public String getHierarchyPath() {
        return null;
    }

    protected void initOperations() {
        if (opsInitialized) {
            return;
        }
        AutomationService service = Framework.getService(AutomationService.class);
        if (service == null) {
            return;
        }
        OperationType[] ops = service.getOperations();
        // make sure operations are ordered, as service currently returns any order
        List<OperationType> oops = Arrays.asList(ops);
        oops.sort(Comparator.comparing(OperationType::getId));
        for (OperationType op : oops) {
            OperationDocumentation documentation;
            try {
                documentation = op.getDocumentation();
            } catch (OperationException e) {
                throw new NuxeoException(e);
            }
            this.operations.add(new OperationInfoImpl(documentation, getVersion(), op.getType().getCanonicalName(),
                    op.getContributingComponent()));
        }
        opsInitialized = true;
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
    public boolean isLatestFT() {
        return false;
    }

    @Override
    public boolean isLatestLTS() {
        return false;
    }

    @Override
    public List<String> getAliases() {
        return aliases;
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    protected List<Plugin<?>> getPlugins() {
        return Framework.getService(SnapshotManager.class).getPlugins();
    }

    @Override
    public ObjectMapper getJsonMapper() {
        ObjectMapper mapper = DistributionSnapshot.jsonMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        for (Plugin<?> plugin : getPlugins()) {
            mapper = plugin.enrishJsonMapper(mapper);
        }
        return mapper;
    }

    @Override
    public void writeJson(OutputStream out) {
        writeJson(out, null);
    }

    protected void writeJson(OutputStream out, PrettyPrinter printer) {
        ObjectWriter writer = getJsonMapper().writerFor(DistributionSnapshot.class)
                                             .withoutRootName()
                                             .with(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM)
                                             .without(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        if (printer != null) {
            writer = writer.with(printer);
        }
        try {
            writer.writeValue(out, this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DistributionSnapshot readJson(InputStream in) {
        ObjectReader reader = getJsonMapper().readerFor(DistributionSnapshot.class)
                                             .withoutRootName()
                                             .without(JsonParser.Feature.AUTO_CLOSE_SOURCE)
                                             .with(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
        try {
            return reader.readValue(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void initPluginSnapshots() {
        if (pluginSnapshotsInitialized) {
            return;
        }
        getPlugins().forEach(plugin -> pluginSnapshots.put(plugin.getId(), plugin.getRuntimeSnapshot(this)));
        pluginSnapshotsInitialized = true;
    }

    @Override
    public Map<String, PluginSnapshot<?>> getPluginSnapshots() {
        initPluginSnapshots();
        return Collections.unmodifiableMap(pluginSnapshots);
    }

    @Override
    public List<BundleInfo> getBundles() {
        return Collections.unmodifiableList(new ArrayList<>(bundles.values()));
    }

}
