/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Sun Seng David TAN <stan@nuxeo.com>
 */
package org.nuxeo.ecm.platform.web.common.locale;

import java.util.Locale;
import java.util.TimeZone;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Provides the default locale and timezone from the server.
 *
 * @since 5.6
 */
public class DefaultLocaleProvider implements LocaleProvider {

    @Override
    public Locale getLocale(CoreSession repo) {
        return Locale.getDefault();
    }

    @Override
    public Locale getLocale(DocumentModel userProfileDoc) {
        return Locale.getDefault();
    }

    @Override
    public TimeZone getTimeZone(CoreSession repo) {
        return TimeZone.getDefault();
    }

    @Override
    public Locale getLocaleWithDefault(CoreSession session) {
        return Locale.getDefault();
    }

    @Override
    public Locale getLocaleWithDefault(DocumentModel userProfileDoc) {
        return Locale.getDefault();
    }

}
