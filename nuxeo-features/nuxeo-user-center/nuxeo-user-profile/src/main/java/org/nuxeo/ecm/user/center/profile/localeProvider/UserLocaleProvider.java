/*
 * (C) Copyright 2012-2018 Nuxeo (http://nuxeo.com/) and others.
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

import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.web.common.locale.DefaultLocaleProvider;
import org.nuxeo.ecm.user.center.profile.UserProfileConstants;
import org.nuxeo.ecm.user.center.profile.UserProfileService;
import org.nuxeo.runtime.api.Framework;

/**
 * Provides user local stored in profile doc model
 *
 * @since 5.6
 */
public class UserLocaleProvider extends DefaultLocaleProvider {

    public static final Log log = LogFactory.getLog(UserLocaleProvider.class);

    @Override
    public Locale getLocale(CoreSession repo) {
        try {
            UserProfileService userProfileService = Framework.getService(UserProfileService.class);
            DocumentModel userProfileDoc = userProfileService.getUserProfileDocument(repo);
            return getLocale(userProfileDoc);
        } catch (Exception ex) {
            log.error("Can't get Locale", ex);
            return null;
        }
    }

    @Override
    public Locale getLocale(DocumentModel userProfileDoc) {
        if (userProfileDoc == null) {
            return null;
        }

        String locale = (String) userProfileDoc.getPropertyValue(UserProfileConstants.USER_PROFILE_LOCALE);
        if (locale == null || locale.trim().length() == 0) {
            // undefined if not set
            return null;
        }
        try {
            return LocaleUtils.toLocale(locale);
        } catch (IllegalArgumentException e) {
            log.error("Locale parse exception:  \"" + locale + "\"", e);
        }
        return null;
    }

    @Override
    public TimeZone getTimeZone(CoreSession repo) {
        // the timezone is not retrieved from the user profile (cookie and Seam
        // TimezoneSelector)
        return null;
    }

    @Override
    public Locale getLocaleWithDefault(CoreSession session) {
        Locale locale = getLocale(session);
        return locale == null ? getDefaultLocale() : locale;
    }

    @Override
    public Locale getLocaleWithDefault(DocumentModel userProfileDoc) {
        Locale locale = getLocale(userProfileDoc);
        return locale == null ? getDefaultLocale() : locale;
    }

}
