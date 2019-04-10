/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ecm.platform.routing.core.io;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.util.Map.Entry;

import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.routing.core.impl.jsongraph.JsonGraphRoute;

import com.fasterxml.jackson.core.JsonGenerator;

@Setup(mode = SINGLETON, priority = REFERENCE)
public class GraphRouteWriter extends ExtensibleEntityJsonWriter<JsonGraphRoute> {

    public GraphRouteWriter() {
        super(ENTITY_TYPE, JsonGraphRoute.class);
    }

    public static final String ENTITY_TYPE = "graph";

    @Override
    public void writeEntityBody(JsonGraphRoute item, JsonGenerator jg) throws IOException {
        for (Entry<String, Object> e : item.getGraphElements().entrySet()) {
            jg.writeObjectField(e.getKey(), e.getValue());
        }
    }

}
