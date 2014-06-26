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

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.ALL_FIELDS;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.BINARYTEXT_FIELD;


/**
 * XMap descriptor for configuring an index
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
@XObject(value = "elasticSearchIndex")
public class ElasticSearchIndexConfig {

    @XNode("@name")
    protected String name;

    @XNode("@repository")
    protected String repositoryName = "default";

    @XNode("@type")
    protected String type = "doc";

    @XNode("@create")
    protected boolean create = true;

    @XNode("settings")
    protected String settings;

    @XNode("mapping")
    protected String mapping;

    @XNodeList(value = "fetchFromSource/exclude", type = String[].class,
            componentType = String.class)
    protected String[] excludes;

    @XNodeList(value = "fetchFromSource/include", type = String[].class,
            componentType = String.class)
    protected String[] includes;

    public String[] getExcludes() {
        if (excludes == null) {
            return new String[] { BINARYTEXT_FIELD };
        }
        return excludes;
    }

    public String[] getIncludes() {
        if (includes == null || includes.length == 0) {
            return new String[] { ALL_FIELDS };
        }
        return includes;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
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

    public String getRepositoryName() {
        return repositoryName;
    }
    /**
     * Uses settings, mapping fields if not defined.
     */
    public void merge(final ElasticSearchIndexConfig other) {
        if (other == null) {
            return;
        }
        if (mapping == null) {
            mapping = other.getMapping();
        }
        if (settings == null) {
            settings = other.getSettings();
        }
    }

}
