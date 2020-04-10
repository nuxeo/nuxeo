/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.apidoc.plugin;

import java.io.IOException;

import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Handles mapping between the plugin snapshot id and actual Java implementation.
 *
 * @since 11.1
 */
public class PluginSnapshotIdResolver extends TypeIdResolverBase {

    @Override
    public String idFromValue(Object value) {
        return getId(value);
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        return getId(value);
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.CUSTOM;
    }

    protected String getId(Object value) {
        if (value instanceof PluginSnapshot<?>) {
            return ((PluginSnapshot<?>) value).getPluginId();
        }
        return value.getClass().getSimpleName();
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) throws IOException {
        SnapshotManager sm = Framework.getService(SnapshotManager.class);
        Plugin<?> plugin = sm.getPlugin(id);
        if (plugin != null) {
            return TypeFactory.defaultInstance().constructFromCanonical(plugin.getPluginSnapshotClass());
        }
        return TypeFactory.unknownType();
    }

}