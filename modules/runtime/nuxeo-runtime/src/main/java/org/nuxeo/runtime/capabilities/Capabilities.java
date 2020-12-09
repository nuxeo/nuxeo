/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.runtime.capabilities;

import java.util.Map;

/**
 * Simple POJO holding the capabilities registered during Nuxeo startup.
 *
 * @since 11.5
 */
public class Capabilities {

    protected final Map<String, Map<String, Object>> capabilities;

    protected Capabilities(Map<String, Map<String, Object>> capabilities) {
        this.capabilities = capabilities;
    }

    public Map<String, Object> get(String key) {
        return capabilities.getOrDefault(key, Map.of());
    }

    public Map<String, Map<String, Object>> get() {
        return capabilities;
    }
}
