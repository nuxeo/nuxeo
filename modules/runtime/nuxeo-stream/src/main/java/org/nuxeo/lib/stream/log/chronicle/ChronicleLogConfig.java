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
package org.nuxeo.lib.stream.log.chronicle;

import java.nio.file.Path;
import java.util.List;

import org.nuxeo.lib.stream.log.AbstractLogConfig;

/**
 * @since 11.1
 */
public class ChronicleLogConfig extends AbstractLogConfig {

    protected final String name;

    protected final Path basePath;

    protected final ChronicleRetentionDuration retention;

    public ChronicleLogConfig(String name, boolean defaultConfig, List<String> patterns, Path basePath,
            String retention) {
        super(defaultConfig, patterns);
        this.name = name;
        this.basePath = basePath;
        this.retention = new ChronicleRetentionDuration(retention);
    }

    public Path getBasePath() {
        return basePath;
    }

    public ChronicleRetentionDuration getRetention() {
        return retention;
    }

    @Override
    public String toString() {
        return "ChronicleLogConfig{" + "name='" + name + '\'' + ", basePath=" + basePath + ", retention=" + retention
                + '}';
    }
}
