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
 * $Id: ResolvedDataImpl.java 28460 2008-01-03 15:34:05Z sfermigier $
 */

package org.nuxeo.ecm.core.search.api.backend.indexing.resources.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedData;

/**
 * Resolved data implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class ResolvedDataImpl implements ResolvedData {

    private static final long serialVersionUID = 7425340080520261538L;

    protected String name;

    protected String analyzerName;

    protected String typeName;

    protected Object value;

    protected boolean stored = false;

    protected boolean indexed = true;

    protected boolean binary = false;

    protected boolean multiple = false;

    protected Map<String, String> termVector = Collections.emptyMap();

    protected Map<String, Serializable> properties = Collections.emptyMap();

    private boolean sortable = true;

    private String sortOption;

    public ResolvedDataImpl() {
    }

    /**
     * @deprecated Use {@link #ResolvedDataImpl(String,String,String,Object,boolean,boolean,boolean,boolean,Map,boolean,Map,String)} instead
     */
    @Deprecated
    public ResolvedDataImpl(String name, String analyzerName, String typeName,
            Object value, boolean stored, boolean indexed, boolean multiple,
            boolean sortable, Map<String, String> termVector, boolean binary,
            Map<String, Serializable> properties) {
        this(name, analyzerName, typeName, value, stored, indexed,
                multiple, sortable, null, termVector, binary,
                properties);
    }

    public ResolvedDataImpl(String name, String analyzerName, String typeName,
            Object value, boolean stored, boolean indexed, boolean multiple,
            boolean sortable, String sortOption, Map<String, String> termVector,
            boolean binary, Map<String, Serializable> properties) {
        this.name = name;
        this.analyzerName = analyzerName;
        this.typeName = typeName;
        this.value = value;
        this.stored = stored;
        this.indexed = indexed;
        if (termVector != null) {
            this.termVector = termVector;
        }
        this.binary = binary;
        this.multiple = multiple;
        this.sortable = sortable;
        if (properties != null) {
            this.properties = properties;
        }
        if (sortOption != null) { // otherwise keep default value
            this.sortOption = sortOption;
        }
    }

    public String getAnalyzerName() {
        return analyzerName;
    }

    public String getName() {
        return name;
    }

    public String getTypeName() {
        return typeName;
    }

    public Object getValue() {
        return value;
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

    public boolean isBinary() {
        return binary;
    }

    public boolean isMultiple() {
        return multiple;
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

}
