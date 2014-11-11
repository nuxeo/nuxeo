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
package org.nuxeo.ecm.platform.web.common.locale;

import java.util.Locale;
import java.util.TimeZone;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Provides the default locale and timezone from the server.
 *
 * @since 5.6
 */
public class DefaultLocaleProvider implements LocaleProvider {

    @Override
    public Locale getLocale(CoreSession repo) throws ClientException {
        return Locale.getDefault();
    }

    @Override
    public Locale getLocale(DocumentModel userProfileDoc) {
        return Locale.getDefault();
    }

    @Override
    public TimeZone getTimeZone(CoreSession repo) throws ClientException {
        return TimeZone.getDefault();
    }

}
