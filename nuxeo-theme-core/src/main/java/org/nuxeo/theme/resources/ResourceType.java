/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.resources;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;

@XObject("resource")
public final class ResourceType implements Type {

    @XNode("@name")
    public String name;

    @XNode("path")
    public String path;

    @XNode("url")
    public String url;

    public String contextPath;

    @XNode("shrinkable")
    public boolean shrinkable = true;

    @XNodeList(value = "require", type = String[].class, componentType = String.class)
    public String[] dependencies;

    public ResourceType() {
    }

    public ResourceType(String name, String path, String[] dependencies) {
        this.name = name;
        this.path = path;
        this.dependencies = dependencies;
    }

    @Override
    public TypeFamily getTypeFamily() {
        return TypeFamily.RESOURCE;
    }

    @Override
    public String getTypeName() {
        return name;
    }

    @XNode("context-path")
    public void setContextPath(String contextPath) {
        this.contextPath = Framework.expandVars(contextPath);
    }

    public String[] getDependencies() {
        return dependencies;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public boolean isShrinkable() {
        return shrinkable;
    }

    public String getUrl() {
        return url;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setShrinkable(boolean shrinkable) {
        this.shrinkable = shrinkable;
    }

    public void setDependencies(String[] dependencies) {
        this.dependencies = dependencies;
    }

}
