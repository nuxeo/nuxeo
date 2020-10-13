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
package org.nuxeo.ecm.platform.audit.stream;

import org.apache.avro.reflect.Nullable;

/**
 * The audit domain event.
 *
 * @since 11.4
 */
public class AuditDomainEvent {

    public AuditDomainEvent() {
        // Required
    }

    public String source;

    public String name;

    @Nullable
    public String category;

    // unix timestamp in UTC
    @Nullable
    public long date;

    // Documents
    @Nullable
    public String docRepository;

    @Nullable
    public String docId;

    @Nullable
    public String docLifeCycle;

    @Nullable
    public String docPath;

    @Nullable
    public String docType;

    // others
    @Nullable
    public String principalName;

    @Nullable
    public String comment;

    @Nullable
    public String extendedInfoAsJson;

    @Override
    public String toString() {
        return "AuditDomainEvent{" + //
                "source='" + source + '\'' + //
                ", name='" + name + '\'' + //
                ", category='" + category + '\'' + //
                ", date=" + date + //
                ", docRepository='" + docRepository + '\'' + //
                ", docId='" + docId + '\'' + //
                ", docLifeCycle='" + docLifeCycle + '\'' + //
                ", docPath='" + docPath + '\'' + //
                ", docType='" + docType + '\'' + //
                ", principalName='" + principalName + '\'' + //
                ", comment='" + comment + '\'' + //
                ", extendedInfoAsJson='" + extendedInfoAsJson + '\'' + //
                '}';
    }
}
