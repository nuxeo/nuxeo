/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     tiry
 *     Andrei Nechaev
 */
package org.nuxeo.ecm.core.event.pipe;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

/**
 * XMap Descriptor for contributing a new {@link EventBundlePipe}
 *
 * @since 8.4
 */
@XObject("eventPipe")
@XRegistry
public class EventPipeDescriptor {

    @XNode("@name")
    @XRegistryId
    protected String name;

    @XNode("@priority")
    protected Integer priority;

    public EventPipeDescriptor() {
    }

    public EventPipeDescriptor(String name, Class<? extends EventBundlePipe> clazz) {
        this.name = name;
        this.clazz = clazz;
    }

    @XNodeMap(value = "parameters/parameter", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> parameters = new HashMap<>();

    /**
     * The implementation class.
     */
    @XNode("@class")
    protected Class<? extends EventBundlePipe> clazz;

    public String getName() {
        return name;
    }

    public Integer getPriority() {
        if (priority == null) {
            return 100;
        }
        return priority;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public EventBundlePipe getInstance() {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

}
