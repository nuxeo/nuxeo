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
package org.nuxeo.ecm.webapp.locale;

import java.util.Locale;
import java.util.TimeZone;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.international.LocaleSelector;
import org.jboss.seam.international.TimeZoneSelector;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webapp.helpers.StartupHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * @author matic
 *
 * Initialize the locale when the user session is entered. Enables client to send their timezone id
 * through AJAX (not yet implemented).
 *
 */
@Name("clientLocaleInitializer")
@Scope(ScopeType.SESSION)
@Startup
public class LocaleStartup {

    public static LocaleStartup instance() {
        if (!Contexts.isSessionContextActive()) {
            return null;
        }
        return (LocaleStartup) Component.getInstance(
                LocaleStartup.class, ScopeType.SESSION);
    }

    @In(create = true)
    protected CoreSession repo;

    // {
    // LogFactory.getLog(ClientLocaleInitializer.class).debug("Time Zone Session Initializer created");
    // }

    protected String tzId;

    public String getTzId() {
        return tzId;
    }

    public void setTzId(String id) {
        tzId = id;
    }

    public void saveUserTimeZone() throws ClientException {
        TimeZone tz = TimeZone.getTimeZone(tzId);
        TimeZoneSelector.instance().selectTimeZone(tz.getID());
    }

    @Observer(StartupHelper.EVENT_TYPE)
    public void handleUserSessionStarted(CoreSession repo) {
        setupTimeZone();
        setupLocale();
    }

    public void setupTimeZone() {
        TimeZoneSelector tzSelector = TimeZoneSelector.instance();
        tzSelector.setCookieEnabled(false);
        TimeZone tz = Framework.getLocalService(TimeZone.class);
        if (tz != null) {
            TimeZoneSelector.instance().selectTimeZone(tz.getID());
        }
    }

    public void setupLocale() {
        LocaleSelector localeSelector = LocaleSelector.instance();
        localeSelector.setCookieEnabled(false);
        Locale locale = Framework.getLocalService(Locale.class);
        if (localeSelector != null) {
            localeSelector.setLocale(locale);
        }
    }

}
