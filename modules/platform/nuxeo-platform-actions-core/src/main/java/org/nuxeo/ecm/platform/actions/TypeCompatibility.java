/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href=mailto:vpasquier@nuxeo.com>Vladimir Pasquier</a>
 */
package org.nuxeo.ecm.platform.actions;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

/**
 * UI type action compatibility descriptor from category action.
 *
 * @since 5.6
 */
@XObject("typeCompatibility")
@XRegistry(compatWarnOnMerge = true)
public class TypeCompatibility {

    @XNode("@type")
    @XRegistryId
    String type;

    @XNodeList(value = "category", type = ArrayList.class, componentType = String.class)
    private List<String> categories = new ArrayList<>();

    public String getType() {
        return type;
    }

    public List<String> getCategories() {
        return categories;
    }

}
