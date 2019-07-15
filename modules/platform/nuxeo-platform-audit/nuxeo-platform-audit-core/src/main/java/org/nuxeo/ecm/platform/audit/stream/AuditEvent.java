/*
 * (C) Copyright 2019 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.audit.stream;

import org.apache.avro.reflect.Nullable;

/**
 * @since 11.1
 */
public class AuditEvent {

    public AuditEvent() {
    }

    @Nullable
    public String source;

    @Nullable
    public String eventId;

    @Nullable
    public String category;

    @Nullable
    public String docId;

    @Nullable
    public String repository;

    @Nullable
    public String principalName;

    @Nullable
    public String docLifeCycle;

    @Nullable
    public String docPath;

    @Nullable
    public String comment;

    @Nullable
    public String docType;

    @Nullable
    public String extendedInfoAsJson;

    public long eventDate;

}
