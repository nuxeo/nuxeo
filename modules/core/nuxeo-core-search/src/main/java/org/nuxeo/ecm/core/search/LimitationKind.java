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
 */
package org.nuxeo.ecm.core.search;

/**
 * Category of search limitation. Enables consumers to distinguish root causes and take appropriate action.
 *
 * @since 2025.17
 */
public enum LimitationKind {

    /**
     * Client does not support this capability (e.g. aggregate type sum, highlight, multi-repositories). The message
     * provides the detail.
     */
    UNSUPPORTED,

    /**
     * Index mapping does not support the requested operation (e.g. field not found, missing lowercase mapping for
     * ILIKE).
     */
    INDEX_MAPPING,

    /**
     * Specific operator not supported for this field (e.g. STARTSWITH only on ecm:path).
     */
    OPERATOR_NOT_SUPPORTED
}
