/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.core.descriptors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.serialize.OutputFormat;
import org.nuxeo.common.xmap.DOMSerializer;
import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.w3c.dom.DocumentFragment;

/**
 * Common descriptor for target packages/platforms.
 *
 * @since 5.7.1
 */
@SuppressWarnings("deprecation")
public class TargetDescriptor {

    private static final Log log = LogFactory.getLog(TargetDescriptor.class);

    @XNode("@id")
    String id;

    @XNode("@enabled")
    Boolean enabled;

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
            this.description = DOMSerializer.toString(descriptionDOM, of).trim();
        } catch (IOException e) {
            log.error(e, e);
        }
    }

    @XNodeList(value = "types/type", type = ArrayList.class, componentType = String.class)
    List<String> types;

    public boolean isEnableSet() {
        return enabled != null;
    }

    public boolean isEnabled() {
        return enabled == null || Boolean.TRUE.equals(enabled);
    }

    // needed for contributions merge
    public void setEnabled(boolean enabled) {
        this.enabled = Boolean.valueOf(enabled);
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

    @Override
    public TargetDescriptor clone() {
        TargetDescriptor clone = new TargetDescriptor();
        doClone(clone);
        return clone();
    }

    protected void doClone(TargetDescriptor clone) {
        clone.id = id;
        clone.enabled = enabled;
        clone.restricted = restricted;
        clone.deprecated = deprecated;
        clone.parent = parent;
        clone.name = name;
        clone.version = version;
        clone.refVersion = refVersion;
        clone.label = label;
        clone.status = status;
        clone.releaseDate = releaseDate;
        clone.endOfAvailability = endOfAvailability;
        clone.downloadLink = downloadLink;
        clone.description = description;
        if (types != null) {
            clone.types = new ArrayList<>(types);
        }
    }
}
