/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.quota;

import static org.nuxeo.ecm.core.api.versioning.VersioningService.DISABLE_AUTO_CHECKOUT;
import static org.nuxeo.ecm.core.api.versioning.VersioningService.VERSIONING_OPTION;
import static org.nuxeo.ecm.platform.audit.service.NXAuditEventsService.DISABLE_AUDIT_LOGGER;
import static org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener.DISABLE_DUBLINCORE_LISTENER;
import static org.nuxeo.ecm.platform.ec.notification.NotificationConstants.DISABLE_NOTIFICATION_SERVICE;
import static org.nuxeo.ecm.platform.htmlsanitizer.HtmlSanitizerListener.DISABLE_HTMLSANITIZER_LISTENER;
import static org.nuxeo.ecm.platform.publisher.listeners.DomainEventsListener.DISABLE_DOMAIN_LISTENER;
import static org.nuxeo.ecm.quota.size.DocumentsSizeUpdater.DISABLE_QUOTA_CHECK_LISTENER;

import java.util.Arrays;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;

public class QuotaUtils {

    private QuotaUtils() {
    }

    protected static final List<String> FLAGS = Arrays.asList( //
            DISABLE_QUOTA_CHECK_LISTENER, //
            DISABLE_AUDIT_LOGGER, //
            DISABLE_DUBLINCORE_LISTENER, //
            DISABLE_NOTIFICATION_SERVICE, //
            DISABLE_AUTO_CHECKOUT, //
            DISABLE_DOMAIN_LISTENER, //
            DISABLE_HTMLSANITIZER_LISTENER);

    /**
     * Disables listeners and options when saving a value in a quota facet.
     *
     * @param doc the document
     * @since 7.4
     */
    public static void disableListeners(DocumentModel doc) {
        for (String flag : FLAGS) {
            doc.putContextData(flag, Boolean.TRUE);
        }
        doc.putContextData(VERSIONING_OPTION, VersioningOption.NONE);
    }

    /**
     * Clears the context data of the various flags used to disable listeners.
     *
     * @param doc the document
     * @since 7.4
     */
    public static void clearContextData(DocumentModel doc) {
        for (String flag : FLAGS) {
            doc.putContextData(flag, null);
        }
        doc.putContextData(VERSIONING_OPTION, null);
    }

}
