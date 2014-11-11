package org.nuxeo.opensocial.dashboard;

import java.util.Locale;

import org.nuxeo.common.utils.i18n.I18NUtils;

public class TranslationHelper {

    public static String getLabel(String key, String lang) {
        Locale locale = new Locale(lang);
        return getLabel(key, locale);
    }

    public static String getLabel(String key, Locale locale) {
        String translated = I18NUtils.getMessageString("messages", key, null,
                locale);
        if (translated == null) {
            translated = I18NUtils.getMessageString("messages", key, null,
                    Locale.ENGLISH);
        }
        return translated;
    }

}
