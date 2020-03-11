/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.core.io;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Represents a REST API version.
 *
 * @since 11.1
 */
public enum APIVersion {

    V1(1), V11(11);

    public static final String API_VERSION_ATTRIBUTE_NAME = "APIVersion";

    public static final Comparator<APIVersion> COMPARATOR = Comparator.comparingInt(v -> v.version);

    public static final Map<Integer, APIVersion> VALID_VERSIONS = Stream.of(values())
                                                                        .sorted(COMPARATOR)
                                                                        .collect(Collectors.toMap(v -> v.version,
                                                                                Function.identity(), (v, w) -> v,
                                                                                LinkedHashMap::new));

    public static final APIVersion LATEST_VERSION = Stream.of(
            values()).max(COMPARATOR).orElseThrow(() -> new NuxeoException("No REST API version found"));

    /**
     * Returns the {@code APIVersion} object for the given {@code version}.
     *
     * @throws NuxeoException if the {@code version} is not a valid REST API version.
     */
    public static APIVersion of(int version) {
        APIVersion apiVersion = VALID_VERSIONS.get(version);
        if (apiVersion == null) {
            throw new NuxeoException(
                    String.format("%s is not part of the valid versions: %s", version, VALID_VERSIONS.keySet()),
                    SC_BAD_REQUEST);
        }
        return apiVersion;
    }

    /**
     * Returns the latest REST API version.
     */
    public static APIVersion latest() {
        return LATEST_VERSION;
    }

    protected final int version;

    APIVersion(int version) {
        this.version = version;
    }

    public int toInt() {
        return version;
    }

    public boolean eq(@NotNull APIVersion other) {
        return this.version == other.version;
    }

    public boolean neq(@NotNull APIVersion other) {
        return this.version != other.version;
    }

    public boolean lt(@NotNull APIVersion other) {
        return this.version < other.version;
    }

    public boolean lte(@NotNull APIVersion other) {
        return this.version <= other.version;
    }

    public boolean gt(@NotNull APIVersion other) {
        return this.version > other.version;
    }

    public boolean gte(@NotNull APIVersion other) {
        return this.version >= other.version;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
