/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.platform.usermanager.io;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;
import static org.nuxeo.ecm.platform.usermanager.io.NuxeoGroupJsonWriter.ENTITY_TYPE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.codehaus.jackson.JsonNode;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.impl.NuxeoGroupImpl;
import org.nuxeo.ecm.core.io.marshallers.json.EntityJsonReader;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.usermanager.UserManager;

/**
 * Convert Json as {@link NuxeoGroup}.
 * <p>
 * Format is (any additional json property is ignored):
 *
 * <pre>
 * {
 *   "entity-type":"group",
 *   "groupname": "GROUP_NAME",
 *   "grouplabel": "GROUP_DISPLAY_NAME",
 *   "memberUsers": [
 *     "USERNAME1",
 *     "USERNAME2",
 *     ...
 *   ],
 *   "memberGroups": [
 *     "GROUPNAME1",
 *     "GROUPNAME2",
 *     ...
 *   ]
 * }
 * </pre>
 *
 * </p>
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class NuxeoGroupJsonReader extends EntityJsonReader<NuxeoGroup> {

    @Inject
    private UserManager userManager;

    public NuxeoGroupJsonReader() {
        super(ENTITY_TYPE);
    }

    @Override
    protected NuxeoGroup readEntity(JsonNode jn) throws IOException {
        NuxeoGroup group = null;
        String id = getStringField(jn, "groupname");
        if (id != null) {
            group = userManager.getGroup(id);
        }
        if (group == null) {
            group = new NuxeoGroupImpl(id);
        }
        String label = getStringField(jn, "grouplabel");
        group.setLabel(label);
        List<String> users = getArrayStringValues(jn.get("memberUsers"));
        group.setMemberUsers(users);
        List<String> groups = getArrayStringValues(jn.get("memberGroups"));
        group.setMemberGroups(groups);
        return group;
    }

    private List<String> getArrayStringValues(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node != null && !node.isNull() && node.isArray()) {
            JsonNode elNode = null;
            Iterator<JsonNode> it = node.getElements();
            while (it.hasNext()) {
                elNode = it.next();
                if (elNode != null && !elNode.isNull() && elNode.isTextual()) {
                    values.add(elNode.getTextValue());
                }
            }
        }
        return values;
    }

}
