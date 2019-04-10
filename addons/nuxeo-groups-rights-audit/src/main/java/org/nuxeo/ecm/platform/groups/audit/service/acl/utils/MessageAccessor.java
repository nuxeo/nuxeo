/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Martin Pernollet
 */
package org.nuxeo.ecm.platform.groups.audit.service.acl.utils;

import java.util.Locale;
import java.util.MissingResourceException;

import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.web.common.locale.LocaleProvider;
import org.nuxeo.runtime.api.Framework;

public class MessageAccessor {

    public static String get(CoreSession session, String key) {
        Locale locale = null;
        LocaleProvider localeProvider = Framework.getService(LocaleProvider.class);
        if (localeProvider != null) {
            locale = localeProvider.getLocale(session);
        }
        if (locale == null) {
            locale = Locale.ENGLISH;
        }

        try {
            return I18NUtils.getMessageString("messages", key, null, locale);
        } catch (MissingResourceException e) {
            return key;
        }
    }

}
