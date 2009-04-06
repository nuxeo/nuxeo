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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Field indexation configuration.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@XObject("field")
public class IndexableFieldDescriptor implements IndexableResourceDataConf {

    private static final long serialVersionUID = 1253760501998888621L;

    @XNode("@name")
    protected String name;

    @XNode("@analyzer")
    protected String indexableAnalyzerName;

    @XNode("@type")
    protected String indexableFieldTypeName;

    @XNode("@stored")
    protected boolean stored;

    @XNode("@indexed")
    protected boolean indexed;

    @XNode("@binary")
    protected boolean binary;

    @XNode("@multiple")
    protected boolean multiple;

    @XNode("@sortable")
    protected boolean sortable;

    @XNode("@sortOption")
    protected String sortOption;

    @XNodeMap(value = "termVector", key = "@propName", type = HashMap.class, componentType = String.class)
    protected Map<String, String> termVector;

    @XNodeMap(value = "properties", key = "@name", type = HashMap.class, componentType = Serializable.class)
    protected Map<String, Serializable> properties;

    public IndexableFieldDescriptor() {
    }

    /**
     * @deprecated Use
     * {@link #IndexableFieldDescriptor(String,String,String,boolean,boolean,boolean,boolean,boolean,String,Map,Map)}
     * instead
     */
    @Deprecated
    public IndexableFieldDescriptor(String name, String indexableAnalyzerName,
            String indexableFieldTypeName, boolean stored, boolean indexed,
            boolean binary, boolean multiple, boolean sortable,
            Map<String, String> termVector,
            Map<String, Serializable> properties) {
        this(name, indexableAnalyzerName, indexableFieldTypeName,
                stored, indexed, binary, multiple, sortable, null,
                termVector, properties);
    }

    public IndexableFieldDescriptor(String name, String indexableAnalyzerName,
            String indexableFieldTypeName, boolean stored, boolean indexed,
            boolean binary, boolean multiple, boolean sortable,
            String sortOption, Map<String, String> termVector, Map<String, Serializable> properties) {
        this.name = name;
        this.indexableAnalyzerName = indexableAnalyzerName;
        this.indexableFieldTypeName = indexableFieldTypeName;
        this.stored = stored;
        this.indexed = indexed;
        this.binary = binary;
        this.multiple = multiple;
        this.sortable = sortable;
        this.termVector = termVector;
        this.properties = properties;
        this.sortOption = sortOption;
    }

    // FIXME: getter and setter names are missmatched
    public String getIndexingName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIndexingAnalyzer() {
        return indexableAnalyzerName;
    }

    public String getIndexingType() {
        return indexableFieldTypeName;
    }

    public boolean isStored() {
        return stored;
    }

    public Map<String, String> getTermVector() {
        return termVector;
    }

    public boolean isIndexed() {
        return indexed;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public boolean isBinary() {
        return binary;
    }

    public boolean isSortable() {
        return sortable;
    }

    public String getSortOption() {
        return sortOption;
    }

    public Map<String, Serializable> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Serializable> properties) {
        this.properties = properties;
    }

}
