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

package org.nuxeo.ecm.core.search.api.indexing.resources.configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Indexable document descriptor.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@XObject("resource")
public class IndexableResourceDescriptor implements IndexableResourceConf {

    private static final long serialVersionUID = -6243415861302877651L;

    @XNode("@name")
    protected String name;

    @XNode("@prefix")
    protected String prefix;

    @XNode("@type")
    protected String type;

    @XNode("@indexAllFields")
    protected boolean allFieldsIndexable = false;

    @XNodeList(value = "excludedField", type = HashSet.class, componentType = String.class)
    protected Set<String> excludedFields = Collections.emptySet();

    @XNodeMap(value = "field", key = "@name", type = HashMap.class, componentType = IndexableFieldDescriptor.class)
    protected Map<String, IndexableResourceDataConf> fields;

    public IndexableResourceDescriptor() {
    }

    public IndexableResourceDescriptor(String name, String prefix,
            boolean allFieldsIndexable, Set<String> excludedFields,
            Map<String, IndexableResourceDataConf> fields, String type) {
        this.name = name;
        this.prefix = prefix;
        this.allFieldsIndexable = allFieldsIndexable;
        this.excludedFields = excludedFields;
        this.fields = fields;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, IndexableResourceDataConf> getIndexableFields() {
        return fields;
    }

    public void setFields(Map<String, IndexableResourceDataConf> fields) {
        this.fields = fields;
    }

    public String getPrefix() {
        return (prefix != null && !prefix.equals("")) ? prefix : name;
    }

    public void setPrefix(String schemaPrefix) {
        prefix = schemaPrefix;
    }

    public boolean areAllFieldsIndexable() {
        return allFieldsIndexable;
    }

    public void setAllFieldsIndexable(boolean allFieldsIndexable) {
        this.allFieldsIndexable = allFieldsIndexable;
    }

    public Set<String> getExcludedFields() {
        return excludedFields;
    }

    public void setExcludedFields(Set<String> excludedFields) {
        this.excludedFields = excludedFields;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format("%s '%s' (type=%s)",
                getClass().getSimpleName(), name, type);
    }

}
