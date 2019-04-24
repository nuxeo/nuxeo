/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.styleguide.service.descriptors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.serialize.OutputFormat;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.xmap.DOMSerializer;
import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.w3c.dom.DocumentFragment;

/**
 * @since 5.7
 */
@XObject("icon")
public class IconDescriptor {

    private static final Log log = LogFactory.getLog(IconDescriptor.class);

    @XNode("@path")
    protected String path;

    @XNode("@enabled")
    protected Boolean enabled = Boolean.TRUE;

    @XNode("label")
    protected String label;

    // retrieve HTML tags => introspect DOM on setter
    protected String description;

    @XNode("sinceVersion")
    protected String sinceVersion;

    @XNodeList(value = "categories/category", type = ArrayList.class, componentType = String.class)
    protected List<String> categories;

    public String getPath() {
        return path;
    }

    public String getFilename() {
        return FileUtils.getFileName(path);
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public String getSinceVersion() {
        return sinceVersion;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @XContent("description")
    @SuppressWarnings("deprecation")
    public void setDescription(DocumentFragment descriptionDOM) {
        try {
            OutputFormat of = new OutputFormat();
            of.setOmitXMLDeclaration(true);
            this.description = DOMSerializer.toString(descriptionDOM, of).trim();
        } catch (IOException e) {
            log.error(e, e);
        }
    }

    public void setSinceVersion(String sinceVersion) {
        this.sinceVersion = sinceVersion;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String toString() {
        return getPath();
    }

}
