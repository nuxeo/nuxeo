package org.nuxeo.ecm.platform.forms.layout.io;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import net.sf.json.JSONObject;

/**
 * Helper to manage labels translation using the default web message bundles
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.5
 *
 */
public class TranslationHelper {

    protected static Map<String, ResourceBundle> translationBundles = new HashMap<String, ResourceBundle>();

    protected static ResourceBundle getBundle(String lang) {
        if (translationBundles.get(lang) == null) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            translationBundles.put(lang, ResourceBundle.getBundle("messages",
                    new Locale(lang), cl));
        }
        return translationBundles.get(lang);
    }

    public static String getTranslation(String key, String lang) {

        ResourceBundle bundle = getBundle(lang);
        if (bundle == null) {
            bundle = getBundle("en");
        }
        String translation = null;
        if (bundle != null && bundle.containsKey(key)) {
            translation = bundle.getString(key);
        }

        if (translation == null && !lang.equals("en")) {
            if (getBundle("en").containsKey(key)) {
                translation = getBundle("en").getString(key);
            }
        }

        if (translation == null) {
            translation = key;
        }
        return translation;
    }

    public static JSONObject getTranslations(JSONObject labels, String lang) {
        for (Object key : labels.keySet()) {
            String value = labels.getString((String) key);
            value = getTranslation(value, lang);
            labels.put(key, value);
        }
        return labels;
    }
}
