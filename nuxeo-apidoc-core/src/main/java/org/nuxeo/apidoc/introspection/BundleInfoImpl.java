/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.apidoc.introspection;

import java.util.ArrayList;
import java.util.Collection;

import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
public class BundleInfoImpl extends BaseNuxeoArtifact implements BundleInfo {

    protected final String bundleId;
    protected final Collection<ComponentInfo> components;

    protected String fileName;
    protected String manifest; //TODO
    protected String[] requirements;

    protected String groupId;
    protected String artifactId;
    protected String artifactVersion;

    protected BundleGroup bundleGroup;

    public BundleGroup getBundleGroup() {
        return bundleGroup;
    }

    public void setBundleGroup(BundleGroup bundleGroup) {
        this.bundleGroup = bundleGroup;
    }

    protected String location;

    public BundleInfoImpl(String bundleId) {
        this.bundleId = bundleId;
        components = new ArrayList<ComponentInfo>();
    }

    public Collection<ComponentInfo> getComponents() {
        return components;
    }

    public void addComponent(ComponentInfoImpl component) {
        components.add(component);
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getBundleId() {
        return bundleId;
    }

    public String[] getRequirements() {
        return requirements;
    }

    public void setRequirements(String[] requirements) {
        this.requirements = requirements;
    }

    public String getManifest() {
        return manifest;
    }

    public void setManifest(String manifest) {
        this.manifest = manifest;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getArtifactGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getArtifactVersion() {
        return artifactVersion;
    }

    public void setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    @Override
    public String getId() {
        return bundleId;
    }

    public String getVersion() {
        return artifactVersion;
    }

    public String getArtifactType() {
        return TYPE_NAME;
    }

    public String getHierarchyPath() {
        return bundleGroup.getHierarchyPath() + "/" + getId();
    }

}
