/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.descriptors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;

/**
 * Descriptor for a list of layout definitions
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
@XObject("layouts")
public class LayoutDescriptors implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNodeList(value = "layout", type = ArrayList.class, componentType = LayoutDescriptor.class)
    List<LayoutDefinition> layouts;

    public List<LayoutDefinition> getLayouts() {
        return layouts;
    }

}
