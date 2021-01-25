/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.jdbc;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

/**
 * Descriptor for the registration of a QueryMaker.
 */
@XObject(value = "queryMaker")
@XRegistry(enable = false)
public class QueryMakerDescriptor {

    @XNode("@name")
    @XRegistryId
    protected String name = "";

    @XNode(value = XEnable.ENABLE, fallback = "@enabled")
    @XEnable
    public boolean enabled;

    @XNode(value = "", trim = true)
    protected Class<? extends QueryMaker> queryMaker;

    /** @since 11.5 **/
    public String getName() {
        return name;
    }

    /** @since 11.5 **/
    public Class<? extends QueryMaker> getQueryMaker() {
        return queryMaker;
    }

}
