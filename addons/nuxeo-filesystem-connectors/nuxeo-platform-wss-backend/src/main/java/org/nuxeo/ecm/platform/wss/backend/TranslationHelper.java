/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */

package org.nuxeo.ecm.platform.wss.backend;

import java.util.Locale;

import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.wss.servlet.WSSRequest;

public class TranslationHelper {

    public static String getLabel(String key, WSSRequest request) {
        String translated = I18NUtils.getMessageString("messages", key, null, request.getHttpRequest().getLocale());
        if (translated == null) {
            translated = I18NUtils.getMessageString("messages", key, null, Locale.ENGLISH);
        }
        return translated;
    }

}
