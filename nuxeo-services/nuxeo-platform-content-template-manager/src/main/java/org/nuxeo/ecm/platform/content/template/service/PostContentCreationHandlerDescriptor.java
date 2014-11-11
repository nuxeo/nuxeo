/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.content.template.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor of a registered {@link PostContentCreationHandler}.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
@XObject("postContentCreationHandler")
public class PostContentCreationHandlerDescriptor implements Cloneable,
        Comparable<PostContentCreationHandlerDescriptor> {

    @XNode("@name")
    private String name;

    @XNode("@class")
    private Class<PostContentCreationHandler> clazz;

    @XNode("@order")
    private int order = 0;

    @XNode("@enabled")
    private boolean enabled = true;

    public String getName() {
        return name;
    }

    public Class<PostContentCreationHandler> getClazz() {
        return clazz;
    }

    public int getOrder() {
        return order;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setClazz(Class<PostContentCreationHandler> clazz) {
        this.clazz = clazz;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /*
     * Override the Object.clone to make it public
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public int compareTo(PostContentCreationHandlerDescriptor o) {
        int cmp = order - o.order;
        if (cmp == 0) {
            // make sure we have a deterministic sort
            cmp = name.compareTo(o.name);
        }
        return cmp;
    }
}
