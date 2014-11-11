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

package org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Indexable doc type descriptor.
 *
 * <p>
 * Defines what type of resources needs to be taken into consideraton while
 * indexing a Nuxeo core document given its docType.
 * </p>
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
@XObject("indexableDocType")
public class IndexableDocTypeDescriptor implements IndexableDocType {

    private static final long serialVersionUID = -853907314981529862L;

    @XNode("@name")
    protected String type;

    @XNode("@indexAllSchemas")
    protected boolean allSchemasIndexable = false;

    @XNode("@allFieldsSortable")
    protected boolean allFieldsSortable = true;

    @XNodeList(value = "excludedSchema", type = ArrayList.class, componentType = String.class)
    protected List<String> excludedSchemas = Collections.emptyList();

    @XNodeList(value = "resource", type = ArrayList.class, componentType = String.class)
    protected List<String> resources = Collections.emptyList();


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getResources() {
        return resources;
    }

    public void setResources(List<String> resources) {
        this.resources = resources;
    }

    public boolean areAllSchemasIndexable() {
        return allSchemasIndexable;
    }

    public void setAllSchemasIndexable(boolean allSchemasIndexable) {
        this.allSchemasIndexable = allSchemasIndexable;
    }

    public List<String> getExcludedSchemas() {
        return excludedSchemas;
    }

    public void setExcludedSchemas(List<String> excludedSchemas) {
        this.excludedSchemas = excludedSchemas;
    }

    public boolean areAllFieldsSortable() {
        return allFieldsSortable;
    }

    public void setAllFieldsSortable(boolean allFieldsSortable) {
        this.allFieldsSortable = allFieldsSortable;
    }

}
