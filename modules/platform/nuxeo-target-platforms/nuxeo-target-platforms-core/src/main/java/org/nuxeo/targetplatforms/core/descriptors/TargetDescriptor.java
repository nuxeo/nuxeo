/*
 * (C) Copyright 2014-2026 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.targetplatforms.core.descriptors;

import static org.apache.commons.lang3.ObjectUtils.getIfNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xml.serialize.OutputFormat;
import org.nuxeo.common.xmap.DOMSerializer;
import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.runtime.model.Descriptor;
import org.w3c.dom.DocumentFragment;

/**
 * Common descriptor for target packages/platforms.
 *
 * @since 5.7.1
 */
@SuppressWarnings("deprecation")
public class TargetDescriptor implements Descriptor {

    private static final Logger log = LogManager.getLogger(TargetDescriptor.class);

    @XNode("@id")
    protected String id;

    @XNode("@enabled")
    protected Boolean enabled;

    @XNode("@restricted")
    protected Boolean restricted;

    @XNode("@deprecated")
    protected Boolean deprecated;

    @XNode("@parent")
    protected String parent;

    @XNode("name")
    protected String name;

    @XNode("version")
    protected String version;

    @XNode("refVersion")
    protected String refVersion;

    @XNode("label")
    protected String label;

    @XNode("status")
    protected String status;

    @XNode("releaseDate")
    protected String releaseDate;

    @XNode("endOfAvailability")
    protected String endOfAvailability;

    @XNode("downloadLink")
    protected String downloadLink;

    // retrieve HTML tags => introspect DOM on setter
    protected String description;

    @XContent("description")
    public void setDescription(DocumentFragment descriptionDOM) {
        try {
            OutputFormat of = new OutputFormat();
            of.setOmitXMLDeclaration(true);
            description = DOMSerializer.toString(descriptionDOM, of).trim();
        } catch (IOException e) {
            log.error(e, e);
        }
    }

    @XNodeList(value = "types/type", type = ArrayList.class, componentType = String.class)
    protected List<String> types;

    @Override
    public String getId() {
        return id;
    }

    public boolean isEnableSet() {
        return enabled != null;
    }

    public boolean isEnabled() {
        return enabled == null || Boolean.TRUE.equals(enabled);
    }

    public boolean isRestricted() {
        return Boolean.TRUE.equals(restricted);
    }

    public boolean isDeprecated() {
        return Boolean.TRUE.equals(deprecated);
    }

    public String getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getRefVersion() {
        return refVersion;
    }

    public String getLabel() {
        return label;
    }

    public String getStatus() {
        return status;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public String getEndOfAvailability() {
        return endOfAvailability;
    }

    public String getDownloadLink() {
        return downloadLink;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getTypes() {
        return types;
    }

    public boolean matchesType(String type) {
        if (types == null) {
            return false;
        }
        return types.contains(type);
    }

    protected void doMerge(TargetDescriptor merged, TargetDescriptor other) {
        merged.id = id;
        // support merge only for enabled boolean
        merged.enabled = getIfNull(other.enabled, enabled);
        merged.restricted = restricted;
        merged.deprecated = deprecated;
        merged.parent = parent;
        merged.name = name;
        merged.version = version;
        merged.refVersion = refVersion;
        merged.label = label;
        merged.status = status;
        merged.releaseDate = releaseDate;
        merged.endOfAvailability = endOfAvailability;
        merged.downloadLink = downloadLink;
        merged.description = description;
        merged.types = new ArrayList<>(CollectionUtils.emptyIfNull(types));
    }
}
