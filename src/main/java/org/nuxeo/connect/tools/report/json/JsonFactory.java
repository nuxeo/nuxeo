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
package org.nuxeo.connect.tools.report.json;

import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

/**
 *
 *
 * @since 8.3
 */
public class JsonFactory implements JsonGeneratorFactory {

    public final JsonGeneratorFactory factory = setupFactory();

    JsonGeneratorFactory setupFactory() {
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
        ;

        return Json.createGeneratorFactory(new MapBuilder<String, Object>().with(JsonGenerator.PRETTY_PRINTING, true).build());
    }

    @Override
    public JsonGenerator createGenerator(Writer writer) {
        return factory.createGenerator(writer);
    }

    @Override
    public JsonGenerator createGenerator(OutputStream out) {
        return factory.createGenerator(out);
    }

    @Override
    public JsonGenerator createGenerator(OutputStream out, Charset charset) {
        return factory.createGenerator(out, charset);
    }

    @Override
    public Map<String, ?> getConfigInUse() {
        return factory.getConfigInUse();
    }


}
