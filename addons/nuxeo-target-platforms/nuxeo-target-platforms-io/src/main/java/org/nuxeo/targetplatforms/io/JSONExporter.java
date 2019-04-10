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
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.nuxeo.targetplatforms.api.TargetPackage;
import org.nuxeo.targetplatforms.api.TargetPackageInfo;
import org.nuxeo.targetplatforms.api.TargetPlatform;
import org.nuxeo.targetplatforms.api.TargetPlatformInfo;
import org.nuxeo.targetplatforms.api.TargetPlatformInstance;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON exporter for target platforms, packages and related info.
 *
 * @since 5.9.3
 */
public class JSONExporter {

    public static void exportToJson(TargetPlatform tp, OutputStream out, boolean pretty) throws IOException {
        exportToJson((Object) tp, out, pretty);
    }

    public static void exportToJson(List<TargetPlatform> tp, OutputStream out, boolean pretty) throws IOException {
        exportToJson((Object) tp, out, pretty);
    }

    public static void exportToJson(TargetPlatformInfo tpi, OutputStream out, boolean pretty) throws IOException {
        exportToJson((Object) tpi, out, pretty);
    }

    public static void exportInfosToJson(List<TargetPlatformInfo> tpi, OutputStream out, boolean pretty)
            throws IOException {
        exportToJson(tpi, out, pretty);
    }

    public static void exportToJson(TargetPlatformInstance tpi, OutputStream out, boolean pretty) throws IOException {
        exportToJson((Object) tpi, out, pretty);
    }

    public static void exportToJson(TargetPackage tp, OutputStream out, boolean pretty) throws IOException {
        exportToJson((Object) tp, out, pretty);
    }

    public static void exportToJson(TargetPackageInfo tpi, OutputStream out, boolean pretty) throws IOException {
        exportToJson((Object) tpi, out, pretty);
    }

    protected static JsonFactory createFactory() {
        JsonFactory factory = new JsonFactory();
        final ObjectMapper oc = new ObjectMapper(factory);
        oc.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        factory.setCodec(oc);
        return factory;
    }

    protected static void exportToJson(Object object, OutputStream out, boolean pretty) throws IOException {
        JsonFactory factory = createFactory();
        try (JsonGenerator jg = factory.createGenerator(out)) {
            if (pretty) {
                jg.useDefaultPrettyPrinter();
            }
            jg.writeObject(object);
        }
    }

}
