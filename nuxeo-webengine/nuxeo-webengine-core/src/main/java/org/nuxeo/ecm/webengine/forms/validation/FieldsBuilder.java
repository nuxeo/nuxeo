/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.forms.validation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FieldsBuilder {

    protected Map<String, String[]> map;

    public static FieldsBuilder create() {
        return new FieldsBuilder();
    }

    public static FieldsBuilder create(Map<String, String[]> map) {
        return new FieldsBuilder(map);
    }

    public FieldsBuilder() {
        map = new HashMap<>();
    }

    public FieldsBuilder(Map<String, String[]> map) {
        this.map = map;
    }

    public FieldsBuilder put(String key, String... value) {
        map.put(key, value);
        return this;
    }

    public FieldsBuilder put(String key, Collection<String> value) {
        map.put(key, value.toArray(new String[value.size()]));
        return this;
    }

    public Map<String, String[]> fields() {
        return map;
    }
}
