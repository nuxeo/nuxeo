/*
 * (C) Copyright 2019-2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.schema;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

/**
 * Descriptor representing a Nuxeo Property.
 * <p>
 * It maps the xml below:
 *
 * <pre>
 * {@code <property schema="SCHEMA" name="NAME" secured="true" deprecation="deprecated|removed" fallback="NAME" />}
 * </pre>
 *
 * @since 11.1
 */
@XObject("property")
@XRegistry(compatWarnOnMerge = true)
@XRegistryId(value = { "@schema", "@name" })
public class PropertyDescriptor {

    public static final String DEPRECATED = "deprecated";

    public static final String REMOVED = "removed";

    @XNode("@schema")
    protected String schema;

    @XNode("@name")
    protected String name;

    @XNode("@secured")
    public Boolean secured;

    @XNode("@deprecation")
    protected String deprecation;

    @XNode("@fallback")
    protected String fallback;

    public String getId() {
        return schema + ':' + name;
    }

    public String getSchema() {
        return schema;
    }

    public String getName() {
        return name;
    }

    public boolean isSecured() {
        return Boolean.TRUE.equals(secured);
    }

    /**
     * @return {@link #DEPRECATED deprecated}, {@link #REMOVED removed} or null
     */
    public String getDeprecation() {
        return deprecation;
    }

    public boolean isDeprecated() {
        return DEPRECATED.equalsIgnoreCase(deprecation);
    }

    public boolean isRemoved() {
        return REMOVED.equalsIgnoreCase(deprecation);
    }

    public String getFallback() {
        return fallback;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
