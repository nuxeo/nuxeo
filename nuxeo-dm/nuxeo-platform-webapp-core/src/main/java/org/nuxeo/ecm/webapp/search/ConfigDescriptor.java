/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.webapp.search;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
@XObject("config")
public class ConfigDescriptor {

    @XNode("@name")
    protected String name;

    @XNodeList(value = "fieldGroup", type = ArrayList.class, componentType = FieldGroupDescriptor.class)
    protected List<FieldGroupDescriptor> fieldGroups;

    public String getName() {
        return name;
    }

    public List<FieldGroupDescriptor> getFieldGroups() {
        return fieldGroups;
    }

}
