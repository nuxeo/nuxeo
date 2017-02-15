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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.schema;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @since 9.2
 */
@XObject("property")
public class PropertyDeprecationDescriptor {

    @XNode("@schema")
    protected String schema;

    @XNode("@name")
    protected String name;

    @XNode("@fallback")
    protected String fallback;

    @XNode("@deprecated")
    protected boolean deprecated;

    public String getSchema() {
        return schema;
    }

    public String getName() {
        return name;
    }

    public String getFallback() {
        return fallback;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("PropertyDeprecationDescriptor");
        builder.append("(schema=").append(schema).append(", ");
        builder.append("name=").append(name).append(", ");
        if (fallback != null) {
            builder.append("fallback=").append(fallback).append(", ");
        }
        builder.append("deprecated=").append(deprecated).append(")");
        return builder.toString();
    }

}
