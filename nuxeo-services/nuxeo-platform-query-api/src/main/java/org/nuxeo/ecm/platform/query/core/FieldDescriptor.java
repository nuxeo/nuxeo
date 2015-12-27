/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSchema() {
        return schema;
    }

    @Override
    public String getXpath() {
        return xpath;
    }

    /**
     * @since 5.6
     */
    @Override
    public FieldDescriptor clone() {
        FieldDescriptor clone = new FieldDescriptor();
        clone.name = name;
        clone.schema = schema;
        clone.xpath = xpath;
        return clone;
    }

}
