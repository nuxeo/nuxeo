/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Taken from https://github.com/concord/concord-jvm
 */
package org.nuxeo.lib.stream.computation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Extend the metadata to add a mapping for the input and output stream.
 *
 * @since 9.3
 */
public class ComputationMetadataMapping extends ComputationMetadata {

    protected final Map<String, String> mapping;

    protected final Map<String, String> reverseMapping;

    public ComputationMetadataMapping(ComputationMetadata metadata, Map<String, String> mapping) {
        super(mapping.getOrDefault(metadata.name, metadata.name),
                metadata.inputStreams().stream().map(s -> mapping.getOrDefault(s, s)).collect(Collectors.toSet()),
                metadata.outputStreams().stream().map(s -> mapping.getOrDefault(s, s)).collect(Collectors.toSet()));
        this.mapping = mapping;
        reverseMapping = new HashMap<>(mapping.size());
        mapping.forEach((key, value) -> reverseMapping.put(value, key));
    }

    public String map(String name) {
        return mapping.getOrDefault(name, name);
    }

    public String reverseMap(String name) {
        return reverseMapping.getOrDefault(name, name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ComputationMetadataMapping that = (ComputationMetadataMapping) o;
        return Objects.equals(mapping, that.mapping) && Objects.equals(reverseMapping, that.reverseMapping);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), mapping, reverseMapping);
    }
}
