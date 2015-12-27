/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     qlamerand
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.configuration.service;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:troger@nuxeo.com">Quentin Lamerand</a>
 */
@XObject("filter")
public class FilterDescriptor {

    @XNode("@name")
    private String name;

    @XNode("@order")
    private int order;

    @XNode("@icon")
    private String icon;

    @XNode("type")
    private String type;

    @XNode("author")
    private String author;

    @XNodeMap(value = "field", key = "@name", type = HashMap.class, componentType = String.class)
    private Map<String, String> fields;

    public String getName() {
        return name;
    }

    public int getOrder() {
        return order;
    }

    public String getIcon() {
        return icon;
    }

    public String getType() {
        return type;
    }

    public String getAuthor() {
        return author;
    }

    public Map<String, String> getFields() {
        return fields;
    }

}
