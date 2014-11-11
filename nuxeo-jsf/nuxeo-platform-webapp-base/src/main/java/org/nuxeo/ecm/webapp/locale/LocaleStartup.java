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

import java.io.Serializable;
import java.util.Locale;

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
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.web.common.locale.LocaleProvider;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

/**
 * Initialize the locale when the user session is entered. Enables client to
 * send their timezone id through AJAX (not yet implemented).
 *
 * @since 5.6
 */
@Name("clientLocaleInitializer")
@Scope(ScopeType.SESSION)
@Startup
public class LocaleStartup implements Serializable {

    private static final long serialVersionUID = 1L;

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

    protected String tzId;

    public String getTzId() {
        return tzId;
    }

    public void setTzId(String id) {
        tzId = id;
    }

    @Observer(EventNames.USER_SESSION_STARTED)
    public void handleUserSessionStarted(CoreSession session) {
        setupTimeZone(session);
        setupLocale(session);
    }

    /**
     * Getting the timezone from the cookies and initialize Seam timezone. The
     * nxtimezone.js contains methods to set the cookie with the browser
     * timezone.
     */
    public void setupTimeZone(CoreSession session) {
        // Not using LocaleProvider to get persisted timezone because it is too
        // hard to make it works with OpenSocialGadgets.
        // and changing a timezone for a Date in javascript is not trivial.
        TimeZoneSelector tzSelector = TimeZoneSelector.instance();
        tzSelector.setCookieEnabled(true);
        tzSelector.initTimeZone();
    }

    public void setupLocale(CoreSession session) {
        Locale locale = null;
        try {
            locale = Framework.getLocalService(LocaleProvider.class).getLocale(
                    session);
        } catch (ClientException e) {
            log.warn(
                    "Couldn't get locale from LocaleProvider, trying request locale and default locale",
                    e);
        }
        setupLocale(locale);
    }

    /**
     * @since 5.9.5
     */
    public void setupLocale(DocumentModel userProfileDoc) {
        Locale locale = null;
        try {
            locale = Framework.getLocalService(LocaleProvider.class).getLocale(
                    userProfileDoc);
        } catch (ClientException e) {
            log.warn(
                    "Couldn't get locale from LocaleProvider, trying request locale and default locale",
                    e);
        }
        setupLocale(locale);
    }

    protected void setupLocale(Locale locale) {
        if (locale == null) {
            log.debug("Locale not set, falling back to request locale");
            locale = FacesContext.getCurrentInstance().getExternalContext().getRequestLocale();
        }
        if (locale == null) {
            log.debug("Locale not set, falling back to default locale");
            locale = Locale.getDefault();
        }
        LocaleSelector localeSelector = LocaleSelector.instance();
        localeSelector.setLocale(locale);
        localeSelector.setCookieEnabled(true);
        localeSelector.select();
    }

}
