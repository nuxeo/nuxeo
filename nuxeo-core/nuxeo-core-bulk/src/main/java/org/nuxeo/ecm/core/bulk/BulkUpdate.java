/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.ecm.core.bulk;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Object containing a map of keys/values to be updated in the keyValueStore.
 *
 * @since 10.2
 */
public class BulkUpdate implements Serializable {

    private static final long serialVersionUID = 1L;

    protected Map<String, String> values = new HashMap<>();

    public BulkUpdate() {
        // Empty constructor for Avro decoder
    }

    public Map<String, String> getValues() {
        return Collections.unmodifiableMap(values);
    }

    public void setValues(Map<String, String> values) {
        this.values = values;
    }

    public void put(String key, String value) {
        values.put(key, value);
    }

}
