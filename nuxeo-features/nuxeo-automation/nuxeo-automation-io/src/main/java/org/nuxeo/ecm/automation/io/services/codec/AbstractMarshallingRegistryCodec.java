/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nicolas Chapurlat <nc@nuxeo.com>
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.io.services.codec;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonReader;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriter;
import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.Reader;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.runtime.api.Framework;

import java.io.IOException;

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
