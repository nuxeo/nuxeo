/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.common.xmap.registry;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor with id combined from several nodes/attributes.
 *
 * @since 11.5
 */
@XObject("descriptor")
@XRegistry
@XRegistryId(value = { "@name", "@type" }, separator = "/")
public class SampleIdDescriptor {

    @XNode("@name")
    String name;

    @XNode("@type")
    String type;

    @XNode(value = "value", defaultAssignment = "Sample")
    String value;

    public String getId() {
        // compat way of getting the combined value
        return name + "/" + type;
    }

}