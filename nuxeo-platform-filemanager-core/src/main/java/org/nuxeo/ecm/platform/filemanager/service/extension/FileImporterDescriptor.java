/*
 * (C) Copyright 2002 - 2006 Nuxeo SARL <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 *
 * $Id: PluginExtension.java 3036 2006-09-18 17:32:20Z janguenot $
 */

package org.nuxeo.ecm.platform.filemanager.service.extension;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 *
 * @author akalogeropoulos
 */
@XObject("plugin")
public class FileImporterDescriptor implements Serializable {

    public static final List<String> DEFAULT_FILTER = new ArrayList<String>();

    private static final long serialVersionUID = 1L;

    @XNode("@enabled")
    boolean enabled = true;

    @XNode("@name")
    protected String name;

    @XNode("@class")
    protected String className;

    @XNodeList(value = "filter", type = ArrayList.class, componentType = String.class)
    protected List<String> filters = DEFAULT_FILTER;

    @XNode("@filter")
    protected String filter;

    @XNode("@order")
    private Integer order;

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

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public List<String> getFilters() {
        return filters;
    }

    public void setFilters(List<String> filters) {
        this.filters = filters;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Integer getOrder() {
        return order;
    }
}
