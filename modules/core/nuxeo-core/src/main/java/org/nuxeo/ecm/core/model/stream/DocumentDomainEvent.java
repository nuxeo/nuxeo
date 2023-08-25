/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.model.stream;

import java.io.Serializable;

import org.apache.avro.reflect.Nullable;

/**
 * Domain Event for Document.
 *
 * @since 2021.44
 */
public class DocumentDomainEvent implements Serializable {

    private static final long serialVersionUID = 20230822L;

    public String source;

    public String event;

    public String repository;

    // doc identifier
    public String id;

    // doc name
    @Nullable
    public String name;

    // doc type
    public String type;

    // user triggering the event
    @Nullable
    public String user;

    public boolean isVersion;

    public boolean isProxy;

    @Nullable
    public String seriesId;

    public DocumentDomainEvent() {
    }

    @Override public String toString() {
        return "DocumentDomainEvent{" + "source='" + source + '\'' + ", event='" + event + '\'' + ", repository='"
                + repository + '\'' + ", id='" + id + '\'' + ", name='" + name + '\'' + ", type='" + type + '\''
                + ", user='" + user + '\'' + ", isVersion=" + isVersion + ", isProxy=" + isProxy + ", seriesId='"
                + seriesId + '\'' + '}';
    }
}
