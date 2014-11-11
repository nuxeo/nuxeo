/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: PluginExtension.java 28924 2008-01-10 14:04:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.transform.service.extensions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 *
 * @author janguenot
 */
@XObject("plugin")
public class PluginExtension implements Serializable {

    public static final List<String> DEFAULT_MIMETYPES = new ArrayList<String>();

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    String name;

    @XNode("@class")
    String className;

    @XNodeList(value = "sourceMimeType", type = ArrayList.class, componentType = String.class)
    List<String> sourceMimeTypes = DEFAULT_MIMETYPES;

    @XNode("@sourceMimeType")
    String sourceMimeType;

    @XNode("@destinationMimeType")
    String destinationMimeType;

    Map<String, Serializable> defaultOptions = new HashMap<String, Serializable>();

    public PluginExtension() {

    }

    public PluginExtension(String name, String className, String sourceMimeType,
            String destinationMimeType) {
        this.name = name;
        this.className = className;
        this.sourceMimeType = sourceMimeType;
        this.destinationMimeType = destinationMimeType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<String> getSourceMimeTypes() {
        return sourceMimeTypes;
    }

    public void setSourceMimeTypes(List<String> sourceMimeTypes) {
        this.sourceMimeTypes = sourceMimeTypes;
    }

    public String getDestinationMimeType() {
        return destinationMimeType;
    }

    public void setDestinationMimeType(String destinationMimeType) {
        this.destinationMimeType = destinationMimeType;
    }

    public void setDefaultOptions(Map<String, Serializable> defaultOptions) {
        this.defaultOptions = defaultOptions;
    }

    public Map<String, Serializable> getDefaultOptions() {
        return defaultOptions;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(PluginExtension.class.getSimpleName());
        buf.append("{name=");
        buf.append(name);
        buf.append(", className=");
        buf.append(className);
        buf.append('}');

        return buf.toString();
    }
}
