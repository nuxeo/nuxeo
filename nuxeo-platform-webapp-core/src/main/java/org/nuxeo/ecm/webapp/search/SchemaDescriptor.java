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
import org.nuxeo.ecm.core.search.api.client.querymodel.descriptor.FieldDescriptor;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
@XObject("schema")
public class SchemaDescriptor {

    @XNode("@name")
    protected String name;

    @XNodeList(value="field", type = ArrayList.class, componentType = FieldDescriptor.class)
    protected List<FieldDescriptor> fields;

    public List<FieldDescriptor> getFields() {
        return fields;
    }

    public String getName() {
        return name;
    }

}
