/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Laurent Doguin
 */
package org.nuxeo.ecm.core.versioning;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor to contribute default versioning rule for all document types.*
 *
 * @author Laurent Doguin
 * @since 5.4.1
 */
@XObject("defaultVersioningRule")
public class DefaultVersioningRuleDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNodeMap(value = "options", key = "@lifeCycleState", type = HashMap.class, componentType = SaveOptionsDescriptor.class)
    public Map<String, SaveOptionsDescriptor> options = new HashMap<String, SaveOptionsDescriptor>();

    @XNode("initialState")
    public InitialStateDescriptor initialState;

    public Map<String, SaveOptionsDescriptor> getOptions() {
        return options;
    }

    public InitialStateDescriptor getInitialState() {
        return initialState;
    }

}
