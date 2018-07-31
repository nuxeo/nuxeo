/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Luís Duarte <lduarte@nuxeo.com>
 *     Florent Guillaume <fguillaume@nuxeo.com>
 *
 */
package org.nuxeo.ecm.automation.server.jaxrs.batch.handler;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchHandler;
import org.nuxeo.runtime.model.Descriptor;

/**
 * BatchHandler Descriptor
 *
 * @since 10.1
 */
@XObject("batchHandler")
public class BatchHandlerDescriptor implements Descriptor {

    @XNode("name")
    public String name;

    @XNode("class")
    public Class<? extends BatchHandler> klass;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> properties = new HashMap<>();

    @Override
    public String getId() {
        return name;
    }

}
