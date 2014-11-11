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

package org.nuxeo.ecm.platform.query.core;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.query.api.PredicateFieldDefinition;

/**
 * Field descriptor accepting a separate schema and field or a complete xpath.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
@XObject(value = "field")
public class FieldDescriptor implements PredicateFieldDefinition {

    @XNode("@name")
    protected String name;

    @XNode("@schema")
    protected String schema;

    @XNode("@xpath")
    protected String xpath;

    public FieldDescriptor() {
    }

    public FieldDescriptor(String schema, String name) {
        this.name = name;
        this.schema = schema;
    }

    public FieldDescriptor(String xpath) {
        this.xpath = xpath;
    }

    public String getName() {
        return name;
    }

    public String getSchema() {
        return schema;
    }

    public String getXpath() {
        return xpath;
    }

}
