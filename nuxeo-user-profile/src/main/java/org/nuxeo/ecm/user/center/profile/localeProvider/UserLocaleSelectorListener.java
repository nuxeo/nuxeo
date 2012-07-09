/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Sun Seng David TAN <stan@nuxeo.com>
 */
package org.nuxeo.ecm.user.center.profile.localeProvider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.user.center.profile.UserProfileService;
import org.nuxeo.ecm.webapp.locale.LocaleStartup;
import org.nuxeo.runtime.api.Framework;

/**
 * Refresh Faces locale and timezone when the userProfileDocument is updated
 * (and created).
 *
 * @since 5.6
 */
public class UserLocaleSelectorListener implements EventListener {

    public static final Log log = LogFactory.getLog(UserLocaleSelectorListener.class);

    @Override
    public void handleEvent(Event event) throws ClientException {
        DocumentEventContext ctx = (DocumentEventContext) event.getContext();
        DocumentModel userProfileDocument = ctx.getSourceDocument();

        // The document should be the current user profile doc
        if (!userProfileDocument.hasFacet("UserProfile")) {
            return;
        }
        UserProfileService userProfileService = Framework.getLocalService(UserProfileService.class);
        DocumentModel userProfileDoc = userProfileService.getUserProfileDocument(ctx.getCoreSession());
        if (!userProfileDoc.getId().equals(userProfileDocument.getId())) {
            return;
        }

        // performing the locale update
        LocaleStartup localeStartup = LocaleStartup.instance();
        if (localeStartup == null) {
            log.warn("Locale Startup not available. Can't set locale");
            return;
        }
        localeStartup.setupLocale(ctx.getCoreSession());

    }

}
