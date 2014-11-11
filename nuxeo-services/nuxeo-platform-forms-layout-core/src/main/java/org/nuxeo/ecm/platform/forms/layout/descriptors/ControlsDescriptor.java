/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @since 5.7
 */
@XObject("controls")
public class ControlsDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNodeMap(value = "control", key = "@name", type = HashMap.class, componentType = String.class)
    Map<String, String> controls = new HashMap<String, String>();

    public Map<String, Serializable> getControls() {
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.putAll(controls);
        return map;
    }
}
