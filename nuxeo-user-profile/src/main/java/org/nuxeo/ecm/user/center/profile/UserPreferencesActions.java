/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License (LGPL)
 * version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * Contributors: Sun Seng David TAN <stan@nuxeo.com>
 */
package org.nuxeo.ecm.user.center.profile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.webapp.action.WebActionsBean;
import org.nuxeo.ecm.webapp.locale.LocaleStartup;

/**
 * Seam component to manage user preferences editing. UI is showing user
 * preferences in a separate screen than user profile, but storing data the same
 * way it is stored with user profile.
 *
 * @since 5.6
 */
@Name("userPreferencesActions")
@Scope(ScopeType.CONVERSATION)
public class UserPreferencesActions extends UserProfileActions {

    public static final Log log = LogFactory.getLog(UserPreferencesActions.class);

    private static final long serialVersionUID = 1L;

    @In
    WebActionsBean webActions;

    public String navigateToPreferencesPage() {
        webActions.setCurrentTabIds("MAIN_TABS:home,USER_CENTER:Preferences");
        return "view_home";
    }

    /**
     * Reset timezone from the cookie. The cookie need to be setted/reset
     * before. (done in javascript)
     */
    public void resetTimezone() {
        // performing the locale update
        LocaleStartup localeStartup = LocaleStartup.instance();
        if (localeStartup == null) {
            log.warn("Locale Startup not available. Can't set locale");
            return;
        }
        localeStartup.setupLocale(documentManager);
    }

}
