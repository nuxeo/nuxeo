/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.elasticsearch.config;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.annotation.XNodeList;
/**
 * XMap descriptor for configuring an index
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
@XObject(value = "elasticSearchIndex")
public class ElasticSearchIndex {

    @XNode("@name")
    protected String indexName;

    @XNode("@create")
    protected boolean create = true;

    @XNode("@forceUpdate")
    protected boolean forceUpdate = false;

    @XNode("settings")
    protected String settings;

    @XNode("mapping")
    protected String mapping;

    @XNodeList(value = "fulltext/field", type = ArrayList.class, componentType = String.class)
    protected List<String> fulltextFields;

    public List<String> getFulltextFields() {
        return fulltextFields;
    }

    public String getIndexName() {
        return indexName;
    }

    public String getSettings() {
        return settings;
    }

    public String getMapping() {
        return mapping;
    }

    public boolean mustCreate() {
        return create;
    }

    public boolean forceUpdate() {
        return forceUpdate;
    }

}
