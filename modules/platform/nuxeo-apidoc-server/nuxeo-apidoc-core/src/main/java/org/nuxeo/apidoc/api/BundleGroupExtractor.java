/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.apidoc.introspection.BundleGroupImpl;
import org.nuxeo.apidoc.introspection.BundleInfoImpl;

/**
 * Introspects maven groupId and artifactId to generate bundle groups for Nuxeo modules.
 *
 * @since 11.1
 */
public class BundleGroupExtractor {

    private static final Logger log = LogManager.getLogger(BundleGroupExtractor.class);

    protected final Map<String, BundleInfo> bundles;

    protected final String version;

    protected final Map<String, List<String>> mavenGroups = new HashMap<>();

    protected final Map<String, List<String>> mavenSubGroups = new HashMap<>();

    protected final List<BundleGroup> roots = new ArrayList<>();

    protected final Map<String, BundleGroup> groups = new LinkedHashMap<>();

    public static final String VIRTUAL_BUNDLE_GROUP = BundleGroup.PREFIX + "org.nuxeo.misc";

    protected static final List<String> BLACKLIST_SUFFIX = List.of("test", "tests");

    protected static final List<String> BLACKLIST = List.of("org", "org.nuxeo", "org.nuxeo.ecm",
            "org.nuxeo.ecm.platform", "com", "com.nuxeo");

    public BundleGroupExtractor(Map<String, BundleInfo> bundles, String version) {
        this.bundles = bundles;
        this.version = version;
        bundles.values().forEach(this::registerBundle);
        generateGroups(version);
    }

    public List<BundleGroup> getRoots() {
        return roots.stream()
                    .sorted(new NuxeoArtifactComparator())
                    .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    public Map<String, BundleGroup> getGroups() {
        return Collections.unmodifiableMap(groups);
    }

    protected void registerBundle(BundleInfo bundle) {
        String groupId = bundle.getGroupId();
        if (groupId != null) {
            groupId = BundleGroup.PREFIX + groupId;
        }
        String artifactId = bundle.getArtifactId();
        if (groupId == null || artifactId == null) {
            groupId = VIRTUAL_BUNDLE_GROUP;
            ((BundleInfoImpl) bundle).setGroupId(groupId);
        }
        mavenGroups.computeIfAbsent(groupId, k -> new ArrayList<>()).add(bundle.getId());
    }

    protected void generateGroups(String version) {
        // post process bundle groups
        List<String> mvnGroupNames = new ArrayList<>();
        mvnGroupNames.addAll(mavenGroups.keySet());

        for (String mvnGroupName : mvnGroupNames) {
            List<String> artifactIds = mavenGroups.get(mvnGroupName);
            Collections.sort(artifactIds);
            Set<String> subGroups = new LinkedHashSet<>();
            for (String id : artifactIds) {
                if (id.contains(".")) {
                    String suffix = id.substring(id.lastIndexOf(".") + 1);
                    if (BLACKLIST_SUFFIX.contains(suffix.toLowerCase())) {
                        continue;
                    }
                    String grpName = id.substring(0, id.lastIndexOf("."));
                    String grp = BundleGroup.PREFIX + grpName;
                    if (grp.equals(mvnGroupName) || BLACKLIST.contains(grpName.toLowerCase())) {
                        continue;
                    }
                    subGroups.add(grp);
                }
            }
            if (subGroups.size() < 1) {
                // no need to split the maven group into subGroups
            } else {
                for (String grp : subGroups) {
                    List<String> grpArtifactIds = new ArrayList<>();
                    for (String aid : artifactIds) {
                        if (aid.startsWith(grp) || (BundleGroup.PREFIX + aid).startsWith(grp)) {
                            grpArtifactIds.add(aid);
                        }
                    }
                    if (grpArtifactIds.size() > 1) {
                        artifactIds.removeAll(grpArtifactIds);
                        mavenSubGroups.put(grp, grpArtifactIds);
                        artifactIds.add(grp);
                    }
                }
            }
        }

        for (String grpId : mavenGroups.keySet()) {
            buildBundleGroup(grpId, version, true);
        }
    }

    protected List<String> getBundleGroupChildren(String groupId) {
        List<String> res = mavenSubGroups.get(groupId);
        if (res == null) {
            res = mavenGroups.get(groupId);
        }

        if (res != null) {
            return res;
        } else {
            return new ArrayList<>();
        }
    }

    protected BundleGroupImpl buildBundleGroup(String id, String version, boolean isParent) {
        BundleGroupImpl bGroup = new BundleGroupImpl(id, version);
        for (String aid : getBundleGroupChildren(id)) {
            if (aid.startsWith(BundleGroup.PREFIX)) {
                BundleGroupImpl newGroup = buildBundleGroup(aid, version, false);
                bGroup.add(newGroup);
                newGroup.setParentGroup(bGroup);
                newGroup.addParent(bGroup.getId());
                groups.put(aid, newGroup);
            } else {
                bGroup.add(aid);
                BundleInfo bi = bundles.get(aid);
                if (bi instanceof BundleInfoImpl) {
                    ((BundleInfoImpl) bi).setBundleGroup(bGroup);
                }
                try {
                    bGroup.addReadme(bi.getParentReadme());
                } catch (IOException e) {
                    log.error("Error setting readme on bundle group", e);
                }
            }
        }
        if (isParent) {
            roots.add(bGroup);
        }
        groups.put(id, bGroup);
        return bGroup;
    }

}
