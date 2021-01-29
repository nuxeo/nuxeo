/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xml.serialize.OutputFormat;
import org.nuxeo.common.xmap.DOMSerializer;
import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.w3c.dom.DocumentFragment;

/**
 * Common descriptor for target packages/platforms.
 *
 * @since 5.7.1
 */
@SuppressWarnings("deprecation")
public class TargetDescriptor {

    private static final Logger log = LogManager.getLogger(TargetDescriptor.class);

    @XNode("@id")
    @XRegistryId
    String id;

    @XNode(value = XEnable.ENABLE, fallback = "@enabled", defaultAssignment = "true")
    // registry needs to list enabled descriptors
    boolean enabled;

    @XNode("@restricted")
    Boolean restricted;

    @XNode("@deprecated")
    Boolean deprecated;

    @XNode("@parent")
    String parent;

    @XNode("name")
    String name;

    @XNode("version")
    String version;

    @XNode("refVersion")
    String refVersion;

    @XNode("label")
    String label;

    @XNode("status")
    String status;

    @XNode("releaseDate")
    String releaseDate;

    @XNode("endOfAvailability")
    String endOfAvailability;

    @XNode("downloadLink")
    String downloadLink;

    // retrieve HTML tags => introspect DOM on setter
    String description;

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
    List<String> types;

    public boolean isEnabled() {
        return enabled;
    }

    public String getId() {
        return id;
    }

    public boolean isRestricted() {
        return restricted != null && Boolean.TRUE.equals(restricted);
    }

    public boolean isDeprecated() {
        return deprecated != null && Boolean.TRUE.equals(deprecated);
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

}
