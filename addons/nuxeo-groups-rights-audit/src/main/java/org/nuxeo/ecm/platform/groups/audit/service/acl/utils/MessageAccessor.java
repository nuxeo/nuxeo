package org.nuxeo.ecm.platform.groups.audit.service.acl.utils;

import java.util.Locale;
import java.util.MissingResourceException;

import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.web.common.locale.LocaleProvider;
import org.nuxeo.runtime.api.Framework;

public class MessageAccessor {
    protected static LocaleProvider localeProvider = Framework.getLocalService(LocaleProvider.class);

    public static String get(CoreSession session, String key) {
        Locale locale = null;
        if (localeProvider != null)
            locale = localeProvider.getLocale(session);
        if (locale == null)
            locale = Locale.ENGLISH;

        try {
            return I18NUtils.getMessageString("messages", key, null, locale);
        } catch (MissingResourceException e) {
            return key;
        }
    }
}
