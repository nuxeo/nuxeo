/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.versioning;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @since 9.1
 */
@XObject("filter")
public class VersioningFilterDescriptor {

    private static final Log log = LogFactory.getLog(VersioningFilterDescriptor.class);

    @XNode("@id")
    protected String id;

    @XNode("@class")
    protected Class<VersioningPolicyFilter> className;

    @XNodeList(value = "type", componentType = String.class, type = HashSet.class)
    protected Set<String> types = new HashSet<>();

    @XNodeList(value = "facet", componentType = String.class, type = HashSet.class)
    protected Set<String> facets = new HashSet<>();

    @XNodeList(value = "schema", componentType = String.class, type = HashSet.class)
    protected Set<String> schemas = new HashSet<>();

    @XNode("condition")
    protected String condition;

    public String getId() {
        return id;
    }

    public VersioningPolicyFilter newInstance() {
        VersioningPolicyFilter filter = null;
        if (className != null) {
            try {
                filter = className.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                log.error("Class " + className + " could not be instantiated", e);
            }
        } else {
            return new StandardVersioningPolicyFilter(types, facets, schemas, condition);
        }
        return filter;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + id + ')';
    }

}
