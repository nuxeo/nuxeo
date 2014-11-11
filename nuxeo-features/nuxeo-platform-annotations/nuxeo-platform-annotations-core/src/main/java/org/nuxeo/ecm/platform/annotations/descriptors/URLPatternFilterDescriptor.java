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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.descriptors;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author Alexandre Russel
 *
 */
@XObject("urlPatternFilter")
public class URLPatternFilterDescriptor {

    @XNode("@order")
    private String order;

    @XNodeList(value = "deny", componentType = String.class, type = ArrayList.class)
    private List<String> denies;

    @XNodeList(value = "allow", componentType = String.class, type = ArrayList.class)
    private List<String> allows;

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public List<String> getDenies() {
        return denies;
    }

    public void setDenies(List<String> denies) {
        this.denies = denies;
    }

    public List<String> getAllows() {
        return allows;
    }

    public void setAllows(List<String> allows) {
        this.allows = allows;
    }

}
