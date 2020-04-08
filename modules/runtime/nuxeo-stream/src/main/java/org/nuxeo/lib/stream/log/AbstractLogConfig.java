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
 *     bdelbosc
 */
package org.nuxeo.lib.stream.log;

import java.util.List;

/**
 * @since 11.1
 */
public abstract class AbstractLogConfig implements LogConfig {

    protected final List<String> patterns;

    protected final boolean defaultConfig;

    public AbstractLogConfig(boolean defaultConfig, List<String> patterns) {
        this.defaultConfig = defaultConfig;
        if (patterns == null) {
            throw new IllegalArgumentException("patterns required");
        }
        this.patterns = patterns;
    }

    @Override
    public boolean isDefault() {
        return defaultConfig;
    }

    @Override
    public boolean match(Name name) {
        if (patterns.stream().anyMatch(pattern -> name.getUrn().startsWith(pattern))) {
            return true;
        }
        return false;
    }

    @Override
    public boolean match(Name name, Name group) {
        if (patterns.stream().anyMatch(pattern -> group.getUrn().startsWith(pattern))) {
            return true;
        }
        if (match(name)) {
            return true;
        }
        return false;
    }


}
