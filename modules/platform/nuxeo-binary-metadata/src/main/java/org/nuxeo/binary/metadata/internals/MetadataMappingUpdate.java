/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.binary.metadata.internals;

import java.io.Serializable;

/**
 * @since 2021.13
 */
public class MetadataMappingUpdate implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final MetadataMappingDescriptor mapping;

    protected final Direction direction;

    protected final boolean async;

    public MetadataMappingUpdate(MetadataMappingDescriptor mapping, Direction direction, boolean async) {
        this.mapping = mapping;
        this.direction = direction;
        this.async = async;
    }

    public MetadataMappingDescriptor getMapping() {
        return mapping;
    }

    public Direction getDirection() {
        return direction;
    }

    public boolean isAsync() {
        return async;
    }

    @Override
    public String toString() {
        return "MetadataMappingUpdate{mapping=" + mapping + ", direction=" + direction + ", async=" + async + '}';
    }

    enum Direction {
        DOC_TO_BLOB, BLOB_TO_DOC
    }
}
