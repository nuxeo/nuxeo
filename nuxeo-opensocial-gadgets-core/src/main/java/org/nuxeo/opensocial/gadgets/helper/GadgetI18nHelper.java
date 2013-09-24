/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.opensocial.gadgets.helper;

import java.util.Locale;

import org.nuxeo.common.utils.i18n.I18NUtils;

/**
 * Helper class to generate i18n titles/description for gadgets.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class GadgetI18nHelper {

    /**
     *
     */
    private static final String LABEL_KEY_DESCRIPTION_SUFFIX = ".description";

    public static final String LABEL_KEY_PREFIX = "label.gadget.";

    private GadgetI18nHelper() {
        // Helper class
    }

    public static String getI18nGadgetTitle(String gadgetName, Locale locale) {
        if (locale == null) {
            locale = new Locale("en");
        }
        String labelKey = LABEL_KEY_PREFIX + gadgetName;
        String i18nTitle = I18NUtils.getMessageString("messages", labelKey,
                null, locale);
        return !i18nTitle.equals(labelKey) ? i18nTitle : gadgetName;
    }

    /**
     * Returns the localized description of a gadget. If it does not exists, it
     * returns the name of the gadget
     *
     * @param gadgetName name of the gadget
     * @param locale the locale to localize (if null then "en")
     * @return
     *
     * @since 5.8
     */
    public static String getI18nGadgetDescription(String gadgetName,
            Locale locale) {
        if (locale == null) {
            locale = new Locale("en");
        }
        String labelKey = LABEL_KEY_PREFIX + gadgetName
                + LABEL_KEY_DESCRIPTION_SUFFIX;
        String i18nTitle = I18NUtils.getMessageString("messages", labelKey,
                null, locale);
        return !i18nTitle.equals(labelKey) ? i18nTitle : gadgetName;
    }

}
