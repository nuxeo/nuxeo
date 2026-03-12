/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.search;

/**
 * Default implementation of {@link SearchLimitation}.
 *
 * @since 2025.17
 */
record SearchLimitationImpl(LimitationKind kind, SearchClient.Capability affectedCapability, String message)
        implements SearchLimitation {

    @Override
    public LimitationKind getKind() {
        return kind;
    }

    @Override
    public SearchClient.Capability getAffectedCapability() {
        return affectedCapability;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
