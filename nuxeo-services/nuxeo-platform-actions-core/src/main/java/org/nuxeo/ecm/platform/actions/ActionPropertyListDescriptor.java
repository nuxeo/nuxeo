/*
 * (C) Copyright 2006-2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: PropertyListDescriptor.java 26053 2007-10-16 01:45:43Z atchertchian $
 */

package org.nuxeo.ecm.platform.actions;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Action property list descriptor
 *
 * @since 5.6
 */
@XObject("propertyList")
public class ActionPropertyListDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNodeList(value = "value", type = String[].class, componentType = String.class)
    String[] values = new String[0];

    public String[] getValues() {
        return values;
    }

    public ActionPropertyListDescriptor clone() {
        ActionPropertyListDescriptor clone = new ActionPropertyListDescriptor();
        if (values != null) {
            clone.values = values.clone();
        }
        return clone;
    }

}
