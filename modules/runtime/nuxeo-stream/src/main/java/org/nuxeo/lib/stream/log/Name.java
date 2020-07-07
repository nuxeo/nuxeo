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

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * An identifier composed of a namespace and a specific name with 2 string representations:<br>
 * - an uniform resource name (urn) represented as a relative path: {@code namespace/name}<br>
 * - an identifier (id): encode the urn as {@code namespace-name}<br>
 *
 *
 * When there is no namespace, URN and id are identical.
 *
 * @since 11.1
 */
public class Name {
    public static final String NAMESPACE_GLOBAL = "_GLOBAL_";

    public static final String NAMESPACE_URN_SEP = "/";

    public static final String NAMESPACE_ID_SEP = "-";

    protected static final Pattern VALID_NAMESPACE_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");

    protected static final Pattern VALID_LOG_NAME_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_\\-]*");

    protected static final Pattern VALID_LOG_NAME_WITHOUT_NS_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_]*");

    protected final String namespace;

    protected final String name;

    protected final String id;

    protected final String urn;

    private Name(String namespace, String name) {
        checkNameSpace(namespace);
        this.namespace = namespace;
        this.name = name;
        if (NAMESPACE_GLOBAL.equals(namespace)) {
            checkLogNameWithoutNamespace(name);
            this.id = name;
            this.urn = name;
        } else {
            checkLogName(name);
            this.id = namespace + NAMESPACE_ID_SEP + name;
            this.urn = namespace + NAMESPACE_URN_SEP + name;
        }
    }

    public static Name of(String namespace, String name) {
        return new Name(namespace, name);
    }

    public static Name ofUrn(String urn) {
        Objects.requireNonNull(urn, "Null URN");
        int pos = urn.indexOf(NAMESPACE_URN_SEP);
        if (pos < 0) {
            return new Name(NAMESPACE_GLOBAL, urn);
        }
        return new Name(urn.substring(0, pos), urn.substring(pos + 1));
    }

    public static Name ofId(String id) {
        Objects.requireNonNull(id, "Null id");
        int pos = id.indexOf(NAMESPACE_ID_SEP);
        if (pos < 0) {
            return new Name(NAMESPACE_GLOBAL, id);
        }
        return new Name(id.substring(0, pos), id.substring(pos + 1));
    }

    public static String idOfUrn(String urn) {
        return Name.ofUrn(urn).getId();
    }

    public static String urnOfId(String id) {
        return Name.ofId(id).getUrn();
    }

    public String getNamespace() {
        return namespace;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getUrn() {
        return urn;
    }

    @Override
    public String toString() {
        return "Name{id='" + id + "', urn='" + urn + "'}";
    }

    protected static void checkLogName(String name) {
        if (!VALID_LOG_NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid name: '" + name + "'.");
        }
    }

    protected static void checkLogNameWithoutNamespace(String name) {
        if (!VALID_LOG_NAME_WITHOUT_NS_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid name without namespace: '" + name + "'");
        }
    }

    protected static void checkNameSpace(String name) {
        if (!VALID_NAMESPACE_PATTERN.matcher(name).matches() && !NAMESPACE_GLOBAL.equals(name)) {
            throw new IllegalArgumentException("Invalid namespace: '" + name + "'");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Name otherName = (Name) o;
        return Objects.equals(urn, otherName.urn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(urn);
    }
}
