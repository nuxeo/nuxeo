/*
 * (C) Copyright 2022 Nuxeo.
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
 */
package org.nuxeo.lib.stream.log.mem;

import java.util.List;

import org.nuxeo.lib.stream.log.AbstractLogConfig;

public class MemLogConfig extends AbstractLogConfig {

    private final String name;

    public MemLogConfig(String name, boolean defaultConfig, List<String> patterns) {
        super(defaultConfig, patterns);
        this.name = name;
    }

    @Override
    public String toString() {
        return "MemLogConfig{name='" + name + "'}";
    }
}
