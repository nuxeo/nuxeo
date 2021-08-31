/*
 * (C) Copyright 2021 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.api.impl;

/**
 * A Guard to avoid the current thread within a transaction to download the main blob in the following cases:
 * - when loading a document with a content/length property that is unset
 * - when saving a document the previous version is loaded with the blob if content/length is unset
 * - when performing a fulltext extractor job because content/length has been updated
 * The Guard is removed on transaction commit or rollback.
 * This Guard works only on DBS repository implementation.
 *
 * @since 2021.8
 */
public class DownloadBlobGuard {
    protected static final ThreadLocal<Boolean> ENABLED = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * Is the guard enabled?
     */
    public static boolean isEnable() {
        return ENABLED.get();
    }

    /**
     * Enables the guard
     */
    public static void enable() {
        ENABLED.set(Boolean.TRUE);
    }

    /**
     * Disables the guard
     */
    public static void disable() {
        ENABLED.set(Boolean.FALSE);
    }
}
