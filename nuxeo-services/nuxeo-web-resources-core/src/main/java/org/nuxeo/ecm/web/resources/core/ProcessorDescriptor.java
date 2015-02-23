/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.web.resources.core;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.web.resources.api.Processor;

/**
 * @since 7.3
 */
@XObject("processor")
public class ProcessorDescriptor implements Processor {

    @XNode("@name")
    public String name;

    @XNode("@enabled")
    protected Boolean enabled;

    @XNode("class")
    Class<?> klass;

    @XNode("@order")
    protected int order = 0;

    @XNode("@type")
    protected String type;

    @XNode(value = "types@append")
    protected Boolean appendTypes;

    @XNodeList(value = "types/type", type = ArrayList.class, componentType = String.class)
    protected List<String> types;

    public String getName() {
        return name;
    }

    /**
     * Returns true if the enabled element was set on the descriptor, useful for merging.
     */
    public boolean isEnableSet() {
        return enabled != null;
    }

    public boolean isEnabled() {
        return enabled == null || Boolean.TRUE.equals(enabled);
    }

    @Override
    public List<String> getTypes() {
        List<String> types = new ArrayList<String>();
        if (type != null) {
            types.add(type);
        }
        types.addAll(this.types);
        return types;
    }

    @Override
    public int getOrder() {
        return order;
    }

    public Class<?> getTargetProcessorClass() {
        return klass;
    }

    @Override
    public int compareTo(Processor o) {
        int cmp = order - o.getOrder();
        if (cmp == 0) {
            // make sure we have a deterministic sort
            cmp = name.compareTo(o.getName());
        }
        return cmp;
    }

    @Override
    public ProcessorDescriptor clone() {
        ProcessorDescriptor clone = new ProcessorDescriptor();
        clone.name = name;
        clone.enabled = enabled;
        clone.klass = klass;
        clone.order = order;
        clone.type = type;
        clone.appendTypes = appendTypes;
        if (types != null) {
            clone.types = new ArrayList<String>(types);
        }
        return clone;
    }

    public void merge(ProcessorDescriptor other) {
        if (other == null) {
            return;
        }
        if (other.isEnableSet()) {
            enabled = other.enabled;
        }
    }

}
