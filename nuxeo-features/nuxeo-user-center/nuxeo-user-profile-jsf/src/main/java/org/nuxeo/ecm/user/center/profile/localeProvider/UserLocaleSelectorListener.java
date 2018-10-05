/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Sun Seng David TAN <stan@nuxeo.com>
 */
package org.nuxeo.ecm.user.center.profile.localeProvider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.webapp.locale.LocaleStartup;
import org.nuxeo.runtime.api.Framework;

/**
 * Refresh Faces locale and timezone when the userProfileDocument is updated (and created).
 *
 * @since 5.6
 */
public class UserLocaleSelectorListener implements EventListener {

    public static final Log log = LogFactory.getLog(UserLocaleSelectorListener.class);

    @Override
    public void handleEvent(Event event) {
        DocumentEventContext ctx = (DocumentEventContext) event.getContext();
        DocumentModel userProfileDocument = ctx.getSourceDocument();

        // The document should be the current user profile doc
        if (!userProfileDocument.hasFacet("UserProfile")) {
            return;
        }
        // No need to sync Seam session for system user
        if (ctx.getPrincipal() instanceof SystemPrincipal) {
            log.debug("Skip locale update for system user");
            return;
        }

        // if the profile does not belong to the current user
        // => no need to sync Seam session
        UserWorkspaceService uws = Framework.getService(UserWorkspaceService.class);
        DocumentModel userWorkspace = uws.getCurrentUserPersonalWorkspace(ctx.getCoreSession(), userProfileDocument);
        if (!userProfileDocument.getPathAsString().startsWith(userWorkspace.getPathAsString())) {
            return;
        }

        // performing the locale update
        LocaleStartup localeStartup = LocaleStartup.instance();
        if (localeStartup == null) {
            log.warn("Locale Startup not available. Can't set locale");
            return;
        }
        localeStartup.setupLocale(userProfileDocument);

    }

}
