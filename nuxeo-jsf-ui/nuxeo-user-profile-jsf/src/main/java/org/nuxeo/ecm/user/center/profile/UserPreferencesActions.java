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
 * Contributors: Sun Seng David TAN <stan@nuxeo.com>
 */
package org.nuxeo.ecm.user.center.profile;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.webapp.action.WebActionsBean;
import org.nuxeo.ecm.webapp.locale.LocaleStartup;

/**
 * Seam component to manage user preferences editing. UI is showing user preferences in a separate screen than user
 * profile, but storing data the same way it is stored with user profile.
 *
 * @since 5.6
 */
@Name("userPreferencesActions")
@Scope(ScopeType.CONVERSATION)
public class UserPreferencesActions extends UserProfileActions {

    public static final Log log = LogFactory.getLog(UserPreferencesActions.class);

    private static final long serialVersionUID = 1L;

    @In
    protected transient WebActionsBean webActions;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    public String navigateToPreferencesPage() {
        webActions.setCurrentTabIds("MAIN_TABS:home,USER_CENTER:Preferences");
        return "view_home";
    }

    /**
     * Reset timezone from the cookie. The cookie need to be setted/reset before. (done in javascript)
     *
     * @since 5.6
     */
    public void resetTimezone() {
        LocaleStartup localeStartup = LocaleStartup.instance();
        if (localeStartup == null) {
            log.warn("Locale Startup not available. Can't reset time zone");
            facesMessages.add(StatusMessage.Severity.WARN, messages.get("label.userPreferences.resetTimeZone.error"));
            return;
        }
        // performing the locale update
        localeStartup.setupLocale(documentManager);
        // performing time zone update form cookie
        localeStartup.setupTimeZone(documentManager);
        facesMessages.add(StatusMessage.Severity.INFO, messages.get("label.userPreferences.resetTimeZone.done"));
    }

}
