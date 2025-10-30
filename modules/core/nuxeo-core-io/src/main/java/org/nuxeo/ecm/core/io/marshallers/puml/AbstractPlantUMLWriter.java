/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.core.io.marshallers.puml;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.nuxeo.ecm.core.io.marshallers.NuxeoMediaType.TEXT_PLANT_UML;
import static org.nuxeo.ecm.core.io.marshallers.NuxeoMediaType.TEXT_PLANT_UML_TYPE;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.reflect.Supports;

/**
 * @since 2025.12
 */
@Supports(TEXT_PLANT_UML)
public abstract class AbstractPlantUMLWriter<EntityType> implements Writer<EntityType> {

    @Inject
    protected RenderingContext ctx;

    @Override
    public boolean accept(Class<?> clazz, Type genericType, MediaType mediatype) {
        return TEXT_PLANT_UML_TYPE.equals(mediatype);
    }

    @Override
    public final void write(EntityType entity, Class<?> clazz, Type genericType, MediaType mediatype, OutputStream out)
            throws IOException {
        var pumlPrinter = getPumlPrinter(out);
        write(entity, pumlPrinter);
        pumlPrinter.flush();
    }

    protected abstract void write(EntityType entity, PlantUMLPrinter pumlPrinter) throws IOException;

    protected PlantUMLPrinter getPumlPrinter(OutputStream out) {
        if (out instanceof OutputStreamWithPumlWriter casted) {
            return casted.getPumlPrinter();
        }
        return new PlantUMLPrinter(new PrintWriter(out, false, UTF_8));
    }
}
