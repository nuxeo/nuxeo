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
 *     Salem Aouana
 */

package org.nuxeo.ecm.core.helpers;

import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Manages the CRUD actions on documents under retention or legal hold.
 * 
 * @since 11.5
 */
public class RetentionHelper {

    public static final String RETENTION_MODE_PROPERTY = "nuxeo.retention.mode";

    public static final String RETENTION_GOVERNANCE_MODE = "governance";

    public static final String REMOVE_RECORD_PERMISSION = "RemoveRecords";

    protected static final String MAIN_BLOB_XPATH = "content";

    /**
     * Checks if the document's deletion action is allowed. A document can be deleted if:
     * <ul>
     * <li>The document is not under retention / legal hold.</li>
     * <li>The document is under retention / legal hold and the action is made by a granted user under
     * {@code governance} mode</li>
     * </ul>
     *
     * @param document the document to check
     * @param principal the Nuxeo principal
     * @see Document#isUnderRetentionOrLegalHold()
     * @throws DocumentSecurityException if the deletion action is not allowed
     */
    public static void checkDeletion(Document document, NuxeoPrincipal principal) {
        if (!canDelete(document, principal)) {
            throw new DocumentSecurityException(
                    String.format("Cannot remove %s, it is under retention / hold", document.getUUID()));
        }
    }

    /**
     * Checks if the document's main content deletion is allowed. A main content can be deleted if:
     * <ul>
     * <li>The document is not under retention / legal hold.</li>
     * <li>The document is under retention / legal hold and the action is made by a granted user under
     * {@code governance} mode</li>
     * </ul>
     *
     * @param document the document
     * @param principal the Nuxeo principal
     * @see Document#isUnderRetentionOrLegalHold()
     * @throws DocumentSecurityException if the deletion action is not allowed
     */
    public static void checkMainContentDeletion(Document document, NuxeoPrincipal principal) {
        if (!canDelete(document, principal)) {
            throw new DocumentSecurityException(String.format(
                    "Cannot delete blob from document %s, it is under retention / hold", document.getUUID()));
        }
    }

    /**
     * Checks if the document's deletion action is allowed, a document or its main content can be deleted if:
     * <ul>
     * <li>The document is not under retention / legal hold.</li>
     * <li>The document is under retention / legal hold and the action is made by a granted user under
     * {@code governance} mode</li>
     * </ul>
     *
     * @param document the document
     * @param principal the Nuxeo principal
     * @see Document#isUnderRetentionOrLegalHold()
     * @return {@code true} if the we can delete the document, {@code false} otherwise
     */
    public static boolean canDelete(Document document, NuxeoPrincipal principal) {
        if (!document.isUnderRetentionOrLegalHold()) {
            return true;
        }
        String mode = Framework.getService(ConfigurationService.class).getString(RETENTION_MODE_PROPERTY, "");
        SecurityService securityService = Framework.getService(SecurityService.class);
        return RETENTION_GOVERNANCE_MODE.equalsIgnoreCase(mode)
                && securityService.checkPermission(document, principal, REMOVE_RECORD_PERMISSION);
    }

    private RetentionHelper() {
        // no instance allowed
    }
}
