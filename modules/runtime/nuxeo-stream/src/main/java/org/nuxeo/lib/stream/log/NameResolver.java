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

/**
 * @since 11.1
 */
public class NameResolver {

    protected final String prefix;

    protected final int prefixLen;

    public NameResolver(String prefix) {
        this.prefix = (prefix != null) ? prefix : "";
        this.prefixLen = this.prefix.length();
    }

    public String getId(Name name) {
        return prefix + name.getId();
    }

    public Name getName(String id) {
        if (!id.startsWith(prefix)) {
            throw new IllegalArgumentException(String.format("invalid id: %s must starts with prefix %s", id, prefix));
        }
        return Name.ofId(id.substring(prefixLen));
    }

    public String getPrefix() {
        return prefix;
    }
    @Override
    public String toString() {
        return "NameResolver{" + "prefix='" + prefix + '\'' + '}';
    }
}
