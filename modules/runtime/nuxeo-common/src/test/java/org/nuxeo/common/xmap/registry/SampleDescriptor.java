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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Sample descriptor with default expected registry annotations and list/map various cases.
 *
 * @since 11.5
 */
@XObject("descriptor")
@XRegistry
@XRegistryId(value = "@name")
public class SampleDescriptor {

    @XNode("@name")
    public String name;

    @XNode(value = "value", defaultAssignment = "Sample")
    public String value;

    @XNode(value = "bool", defaultAssignment = "true")
    public boolean bool;

    @XNodeList(value = "stringList/item", type = ArrayList.class, componentType = String.class)
    public List<String> stringList;

    @XNodeList(value = "stringListAnnotated/item", type = ArrayList.class, componentType = String.class, nullByDefault = true)
    @XMerge(value = "stringListAnnotated@append", fallback = "stringListAnnotated"
            + XMerge.MERGE, defaultAssignment = true)
    @XRemove("stringListAnnotated@remove")
    public List<String> stringListAnnotated;

    @XNodeMap(value = "stringMap/item", key = "@id", type = HashMap.class, componentType = String.class)
    public Map<String, String> stringMap;

    @XNodeMap(value = "stringMapAnnotated/item", key = "@id", type = HashMap.class, componentType = String.class)
    @XMerge(value = "stringMapAnnotated" + XMerge.MERGE, defaultAssignment = false)
    @XRemove("stringMapAnnotated@empty")
    public Map<String, String> stringMapAnnotated;

}
