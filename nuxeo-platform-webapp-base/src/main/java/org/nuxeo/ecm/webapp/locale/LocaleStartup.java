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
 *     Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 *     Stephane Lacoin at Nuxeo (aka matic) <slacoin@nuxeo.com>
 */
package org.nuxeo.ecm.webapp.locale;

import java.util.Locale;
import java.util.TimeZone;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.nuxeo.ecm.platform.web.common.locale.LocaleProvider;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

/**
 * Initialize the locale when the user session is entered. Enables client to
 * send their timezone id through AJAX (not yet implemented).
 */
@Name("clientLocaleInitializer")
@Scope(ScopeType.SESSION)
@Startup
public class LocaleStartup {

    public static final Log log = LogFactory.getLog(LocaleStartup.class);

    public static LocaleStartup instance() {
        if (!Contexts.isSessionContextActive()) {
            return null;
        }
        return (LocaleStartup) Component.getInstance(LocaleStartup.class,
                ScopeType.SESSION);
    }

    @In(create = true)
    protected CoreSession documentManager;

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

    @Observer(EventNames.USER_SESSION_STARTED)
    public void handleUserSessionStarted(CoreSession session) {
        setupTimeZone(session);
        setupLocale(session);
    }

    public static void setupTimeZone(CoreSession session) {
        TimeZone tz = null;
        try {
            tz = Framework.getLocalService(LocaleProvider.class).getTimeZone(
                    session);
        } catch (ClientException e) {
            log.warn(
                    "Couldn't get timezone from LocaleProvider, trying default timezone",
                    e);
        }
        if (tz == null) {
            log.debug("Timezone not set, falling back to default timezone");
            tz = TimeZone.getDefault();
        }
        TimeZoneSelector tzSelector = TimeZoneSelector.instance();
        tzSelector.setCookieEnabled(false);
        tzSelector.selectTimeZone(tz.getID());
    }

    public static void setupLocale(CoreSession session) {
        Locale locale = null;
        try {
            locale = Framework.getLocalService(LocaleProvider.class).getLocale(
                    session);
        } catch (ClientException e) {
            log.warn(
                    "Couldn't get locale from LocaleProvider, trying request locale and default locale",
                    e);
        }
        if (locale == null) {
            log.debug("Locale not set, falling back to request locale");
            locale = FacesContext.getCurrentInstance().getExternalContext().getRequestLocale();
        }
        if (locale == null) {
            log.debug("Locale not set, falling back to default locale");
            locale = Locale.getDefault();
        }
        LocaleSelector localeSelector = LocaleSelector.instance();
        localeSelector.setCookieEnabled(false);
        localeSelector.setLocale(locale);
    }

}
