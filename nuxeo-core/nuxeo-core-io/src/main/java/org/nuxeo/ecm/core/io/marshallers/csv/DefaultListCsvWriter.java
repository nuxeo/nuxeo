/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.marshallers.csv;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.nuxeo.ecm.core.io.registry.MarshallingException;
import org.nuxeo.ecm.core.io.registry.Writer;

/**
 * @param <EntityType> The type of the element of this list.
 * @since 7.2
 */
public abstract class DefaultListCsvWriter<EntityType> extends AbstractCsvWriter<List<EntityType>> {

    /**
     * The Java type of the element of this list.
     */
    private final Class<EntityType> elClazz;

    /**
     * The generic type of the element of this list.
     */
    private final Type elGenericType;

    /**
     * Use this constructor if the element of the list are not based on Java generic type.
     *
     * @param entityType The list "entity-type".
     * @param elClazz The class of the element of the list.
     */
    public DefaultListCsvWriter(Class<EntityType> elClazz) {
        super();
        this.elClazz = elClazz;
        this.elGenericType = elClazz;
    }

    /**
     * Use this constructor if the element of the list are based on Java generic type.
     *
     * @param entityType The list "entity-type".
     * @param elClazz The class of the element of the list.
     * @param elGenericType The generic type of the list (you can use {@link TypeUtils#parameterize(Class, Type...) to
     *            generate it}
     */
    public DefaultListCsvWriter(Class<EntityType> elClazz, Type elGenericType) {
        super();
        this.elClazz = elClazz;
        this.elGenericType = elGenericType;
    }

    @Override
    public List<CsvContributor<?>> getColumns() {
        AbstractCsvWriter<EntityType> writer = getWriter();
        return writer.getColumns();
    }

    @Override
    public void write(List<EntityType> list, List<CsvContributor<?>> generator, OutputStream out)
            throws IOException {
        AbstractCsvWriter<EntityType> writer = getWriter();
        Iterator<EntityType> it = list.iterator();
        while (it.hasNext()) {
            writer.write(it.next(), generator, out);
            newLine(out);
        }
    }

    private AbstractCsvWriter<EntityType> getWriter() {
        Writer<EntityType> writer = registry.getWriter(ctx, elClazz, elGenericType, MediaType.valueOf("text/csv"));
        if (writer instanceof AbstractCsvWriter) {
            return (AbstractCsvWriter<EntityType>) writer;
        } else {
            throw new MarshallingException("No valid Csv Writer found for " + elClazz.getName());
        }
    }

}
