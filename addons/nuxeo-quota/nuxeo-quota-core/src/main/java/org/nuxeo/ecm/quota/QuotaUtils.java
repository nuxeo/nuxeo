/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.quota;

import static org.nuxeo.ecm.core.versioning.VersioningService.DISABLE_AUTO_CHECKOUT;
import static org.nuxeo.ecm.core.versioning.VersioningService.VERSIONING_OPTION;
import static org.nuxeo.ecm.platform.audit.service.NXAuditEventsService.DISABLE_AUDIT_LOGGER;
import static org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener.DISABLE_DUBLINCORE_LISTENER;
import static org.nuxeo.ecm.platform.ec.notification.NotificationConstants.DISABLE_NOTIFICATION_SERVICE;
import static org.nuxeo.ecm.platform.htmlsanitizer.HtmlSanitizerListener.DISABLE_HTMLSANITIZER_LISTENER;
import static org.nuxeo.ecm.platform.publisher.listeners.DomainEventsListener.DISABLE_DOMAIN_LISTENER;
import static org.nuxeo.ecm.quota.size.QuotaSyncListenerChecker.DISABLE_QUOTA_CHECK_LISTENER;

import java.util.Arrays;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;

public class QuotaUtils {

    private QuotaUtils() {
    }

    public static List<String> FLAGS = Arrays.asList( //
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
        doc.putContextData(DISABLE_QUOTA_CHECK_LISTENER, null);
    }

}
