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
 *     Nicolas Chapurlat <nc@nuxeo.com>
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.io.services.codec;

import java.io.IOException;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonReader;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriter;
import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

/**
 * @since 7.3
 */
public abstract class AbstractMarshallingRegistryCodec<EntityType> extends ObjectCodec<EntityType> {

    String entityType;
    Class<? extends AbstractJsonReader<EntityType>> reader;
    Class<? extends AbstractJsonWriter<EntityType>> writer;

    public AbstractMarshallingRegistryCodec(Class<EntityType> clazz, String entityType,
            Class<? extends AbstractJsonReader<EntityType>> reader, Class<? extends AbstractJsonWriter<EntityType>> writer) {
        super(clazz);
        this.entityType = entityType;
        this.reader = reader;
        this.writer = writer;
    }

    @Override
    public String getType() {
        return entityType;
    }

    @Override
    public boolean isBuiltin() {
        return true;
    }

    @Override
    public void write(JsonGenerator jg, EntityType value) throws IOException {
        MarshallerRegistry registry = Framework.getService(MarshallerRegistry.class);
        AbstractJsonWriter<EntityType> writer = registry.getInstance(null, this.writer);
        writer.write(value,jg);
    }

    @Override
    public EntityType read(JsonParser jp, CoreSession session) throws
            IOException {
        MarshallerRegistry registry = Framework.getService(MarshallerRegistry.class);
        RenderingContext ctx = RenderingContext.CtxBuilder.session(session).get();
        AbstractJsonReader<EntityType> reader= registry.getInstance(ctx, this.reader);
        return reader.read(jp.readValueAsTree());

    }
}
