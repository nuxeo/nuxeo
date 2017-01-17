/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.api.model.impl;

import java.util.HashSet;

import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.schema.Namespace;
import org.nuxeo.ecm.core.schema.types.ComplexTypeImpl;
import org.nuxeo.ecm.core.schema.types.FieldImpl;
import org.nuxeo.ecm.core.schema.types.ListTypeImpl;
import org.nuxeo.ecm.core.schema.types.QName;
import org.nuxeo.ecm.core.schema.types.SchemaImpl;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class })
@Deploy("org.nuxeo.ecm.core.schema")
public abstract class AbstractTestProperty {

    protected ScalarProperty getScalarProperty() {
        SchemaImpl schema = getSchema();
        DocumentPartImpl part = new DocumentPartImpl(schema);
        return new ScalarProperty(part, new FieldImpl(new QName("scalar"), getSchema(), StringType.INSTANCE));
    }

    protected ComplexProperty getComplexProperty() {
        SchemaImpl schema = getSchema();
        DocumentPartImpl part = new DocumentPartImpl(schema);
        ComplexTypeImpl type = new ComplexTypeImpl(schema, "test", "complex");
        type.addField("test1", StringType.INSTANCE, null, 0, new HashSet<>());
        type.addField("test2", StringType.INSTANCE, null, 0, new HashSet<>());
        return new MapProperty(part, new FieldImpl(new QName("test:complex"), schema, type));
    }

    protected ListProperty getListProperty() {
        SchemaImpl schema = getSchema();
        DocumentPartImpl part = new DocumentPartImpl(schema);
        ListTypeImpl type = new ListTypeImpl("test", "list", StringType.INSTANCE, "listItem", null, 0, new HashSet<>(),
                0, -1);
        return new ListProperty(part, new FieldImpl(new QName("test:list"), null, type));
    }

    protected ArrayProperty getArrayProperty() {
        return getArrayProperty(StringType.INSTANCE);
    }

    protected ArrayProperty getArrayProperty(Type childType) {
        SchemaImpl schema = getSchema();
        DocumentPartImpl part = new DocumentPartImpl(schema);
        ListTypeImpl type = new ListTypeImpl("test", "list", childType, null, null, 0, new HashSet<>(), 0, -1);
        return new ArrayProperty(part, new FieldImpl(new QName("test:list"), null, type), 0);
    }

    protected SchemaImpl getSchema() {
        return new SchemaImpl("test", new Namespace("test.com", "http"));
    }

}
