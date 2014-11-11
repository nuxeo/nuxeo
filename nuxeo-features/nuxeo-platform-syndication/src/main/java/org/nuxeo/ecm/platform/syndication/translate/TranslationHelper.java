/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.syndication.translate;

import java.util.Locale;

import org.nuxeo.common.utils.i18n.I18NUtils;

public class TranslationHelper {

    private TranslationHelper() {
    }

    public static String getLabel(String key, String lang) {
        if (key == null || lang == null) {
            return key;
        }
        Locale locale = new Locale(lang);
        return getLabel(key, locale);
    }

    public static String getLabel(String key, Locale locale) {
        String translated = I18NUtils.getMessageString("messages", key, null, locale);
        if (translated==null) {
            translated = I18NUtils.getMessageString("messages", key, null, Locale.ENGLISH);
        }
        return translated;
    }

}
