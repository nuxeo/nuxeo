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
package org.nuxeo.apidoc.introspection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.PackageInfo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @since 11.1
 */
public class PackageInfoImpl extends BaseNuxeoArtifact implements PackageInfo {

    protected final List<String> bundles = new ArrayList<>();

    protected final String id;

    protected final String name;

    protected final String title;

    protected final String version;

    protected final String packageType;

    protected final List<String> dependencies = new ArrayList<>();

    protected final List<String> optionalDependencies = new ArrayList<>();

    protected final List<String> conflicts = new ArrayList<>();

    @JsonCreator
    public PackageInfoImpl(@JsonProperty("id") String id, @JsonProperty("name") String name,
            @JsonProperty("version") String version, @JsonProperty("title") String title,
            @JsonProperty("packageType") String packageType, @JsonProperty("dependencies") List<String> dependencies,
            @JsonProperty("optionalDependencies") List<String> optionalDependencies,
            @JsonProperty("conflicts") List<String> conflicts, @JsonProperty("bundles") List<String> bundles) {
        this.id = id;
        this.name = name;
        this.title = title;
        this.version = version;
        this.packageType = packageType;
        if (dependencies != null) {
            this.dependencies.addAll(dependencies);
        }
        if (optionalDependencies != null) {
            this.optionalDependencies.addAll(optionalDependencies);
        }
        if (conflicts != null) {
            this.conflicts.addAll(conflicts);
        }
        if (bundles != null) {
            this.bundles.addAll(bundles);
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getArtifactType() {
        return TYPE_NAME;
    }

    @Override
    public String getHierarchyPath() {
        return "/" + getId();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getPackageType() {
        return packageType;
    }

    @Override
    public List<String> getBundles() {
        return Collections.unmodifiableList(bundles);
    }

    public void addBundle(String bundle) {
        if (!bundles.contains(bundle)) {
            bundles.add(bundle);
        }
    }

    @Override
    public List<String> getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    @Override
    public List<String> getOptionalDependencies() {
        return Collections.unmodifiableList(optionalDependencies);
    }

    @Override
    public List<String> getConflicts() {
        return Collections.unmodifiableList(conflicts);
    }

}
