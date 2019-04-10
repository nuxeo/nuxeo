/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.opencmis.impl.server.versioning;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.versioning.VersioningPolicyFilter;

/**
 * Automatic versioning filter to filter out document from CMIS (automatic versioning is currently not supported by
 * CMIS).
 * <p />
 * This filter has an enabled state which is a {@link ThreadLocal}, it returns {@link Boolean#TRUE} only for thread
 * from CMIS.
 *
 * @since 9.1
 */
public class CMISVersioningFilter implements VersioningPolicyFilter {

    protected static final ThreadLocal<Boolean> ENABLED = new ThreadLocal<Boolean>() {

        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }

    };

    @Override
    public boolean test(DocumentModel previousDocument, DocumentModel currentDocument) {
        return ENABLED.get().booleanValue();
    }

    /**
     * Enables the filter for current thread, which will disable the automatic versioning.
     */
    public static void enable() {
        ENABLED.set(Boolean.TRUE);
    }

    /**
     * Disables the filter for current thread.
     */
    public static void disable() {
        ENABLED.set(Boolean.FALSE);
    }

}
