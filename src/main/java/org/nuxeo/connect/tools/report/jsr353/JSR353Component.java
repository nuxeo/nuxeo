/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stephane Lacoin at Nuxeo (aka matic)
 */
package org.nuxeo.connect.tools.report.jsr353;

import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonReaderFactory;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Exposes JSR 353 JSON factories as runtime services
 *
 * @since 8.3
 */
public class JSR353Component extends DefaultComponent {

    final Map<String, Object> config = new MapBuilder<String, Object>().with(JsonGenerator.PRETTY_PRINTING, true).build();

    final JsonGeneratorFactory generators = Json.createGeneratorFactory(config);

    final JsonReaderFactory readers = Json.createReaderFactory(config);

    final JsonWriterFactory writers = Json.createWriterFactory(config);

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(JsonGeneratorFactory.class)) {
            return adapter.cast(generators);
        }
        if (adapter.isAssignableFrom(JsonWriterFactory.class)) {
            return adapter.cast(writers);
        }
        if (adapter.isAssignableFrom(JsonReaderFactory.class)) {
            return adapter.cast(readers);
        }
        return super.getAdapter(adapter);
    }

    class MapBuilder<K, V> {
        final Map<K, V> store = new HashMap<>();

        MapBuilder<K, V> with(K key, V value) {
            store.put(key, value);
            return this;
        }

        Map<K, V> build() {
            return store;
        }
    }

}
