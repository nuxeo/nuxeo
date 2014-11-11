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
 *     matic
 */
package org.nuxeo.ecm.platform.web.common.locale;

import java.util.Locale;
import java.util.TimeZone;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Provides locale and timezone.
 *
 * @since 5.6
 */
public interface LocaleProvider {

    /**
     * @return the Locale to be used or null to let the caller decides.
     */
    public Locale getLocale(CoreSession session) throws ClientException;

    /**
     * Gets the locale stored in the given user profile.
     *
     * @return the Locale to be used or null to let the caller decide
     */
    Locale getLocale(DocumentModel userProfileDoc) throws ClientException;

    /**
     * @return the Timezone to be used or null to let the caller decides.
     */
    public TimeZone getTimeZone(CoreSession session) throws ClientException;

}
