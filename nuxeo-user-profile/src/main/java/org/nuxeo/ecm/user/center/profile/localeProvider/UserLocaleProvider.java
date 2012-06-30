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

import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.web.common.locale.LocaleProvider;
import org.nuxeo.ecm.user.center.profile.UserProfileService;
import org.nuxeo.runtime.api.Framework;

/**
 * Provides user local stored in profile doc model
 */
public class UserLocaleProvider implements LocaleProvider {
    public static final Log log = LogFactory.getLog(UserLocaleProvider.class);

    @Override
    public Locale getLocale(CoreSession repo) throws ClientException {
        UserProfileService userProfileService = Framework.getLocalService(UserProfileService.class);
        DocumentModel userProfileDoc = userProfileService.getUserProfileDocument(repo);
        String locale = (String) userProfileDoc.getPropertyValue("userprofile:locale");
        if (locale == null || locale.trim().length() == 0) {
            // undefined if not set
            return null;
        }
        try {
            return LocaleUtils.toLocale(locale);
        } catch (Exception e) {
            log.error("Locale parse exception:  \"" + locale + "\"", e);
        }
        return null;
    }

    @Override
    public TimeZone getTimeZone(CoreSession repo) throws ClientException {
        // the timezone is not retrieved from the user profile (cookie and Seam
        // TimezoneSelector)
        return null;
    }

}
