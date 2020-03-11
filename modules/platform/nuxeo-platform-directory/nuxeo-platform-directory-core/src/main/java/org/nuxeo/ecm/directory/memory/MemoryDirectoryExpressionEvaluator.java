/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.directory.memory;

import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.runtime.api.Framework;

/**
 * Evaluates an expression on a memory directory entry.
 *
 * @since 10.3
 */
public class MemoryDirectoryExpressionEvaluator extends MapExpressionEvaluator {

    protected final String directoryName;

    protected final Schema schema;

    public MemoryDirectoryExpressionEvaluator(Directory directory) {
        directoryName = directory.getName();
        schema = Framework.getService(SchemaManager.class).getSchema(directory.getSchema());
    }

    @Override
    protected QueryParseException unknownProperty(String name) {
        return new QueryParseException("No column: " + name + " for directory: " + directoryName);
    }

    @Override
    public Object walkReference(Reference ref) {
        if (ref.cast != null) {
            throw new QueryParseException("Cannot use cast: " + ref);
        }
        String name = ref.name;
        Field field = schema.getField(name);
        if (field == null) {
            throw unknownProperty(name);
        }
        Object value = map.get(name);
        if (field.getType() instanceof BooleanType) {
            value = value == null ? null : (((Boolean) value).booleanValue() ? ONE : ZERO);
        }
        return value;
    }

}
