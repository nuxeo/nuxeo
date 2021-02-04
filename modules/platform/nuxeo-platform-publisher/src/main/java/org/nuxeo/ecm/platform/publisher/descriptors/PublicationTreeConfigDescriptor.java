/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.descriptors;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

/**
 * Descriptor for a PublicationTree configuration.
 *
 * @author tiry
 */
@XObject("publicationTreeConfig")
@XRegistry(compatWarnOnMerge = true)
public class PublicationTreeConfigDescriptor {

    @XNode("@name")
    @XRegistryId
    private String name;

    @XNode("@tree")
    private String tree;

    @XNode("@title")
    private String title;

    @XNode("@validatorsRule")
    private String validatorsRule;

    @XNode("@factory")
    private String factory;

    @XNodeMap(value = "parameters/parameter", key = "@name", type = HashMap.class, componentType = String.class)
    Map<String, String> parameters = new HashMap<>();

    // needed by xmap
    public PublicationTreeConfigDescriptor() {
    }

    // needed by service API
    /** @since 11.5 */
    public PublicationTreeConfigDescriptor(String name, PublicationTreeConfigDescriptor other) {
        this.name = name;
        tree = other.tree;
        title = other.title;
        validatorsRule = other.validatorsRule;
        factory = other.factory;
        parameters.putAll(other.parameters);
    }

    public String getName() {
        return name;
    }

    public String getTree() {
        return tree;
    }

    public String getValidatorsRule() {
        return validatorsRule;
    }

    public String getFactory() {
        return factory;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getTitle() {
        return title;
    }

}
