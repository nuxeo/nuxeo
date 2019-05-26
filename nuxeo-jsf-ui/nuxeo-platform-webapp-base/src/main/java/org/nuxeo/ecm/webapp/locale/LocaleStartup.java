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
 *     Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 *     Stephane Lacoin at Nuxeo (aka matic) <slacoin@nuxeo.com>
 */
package org.nuxeo.ecm.webapp.locale;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Locale;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.international.LocaleSelector;
import org.jboss.seam.international.TimeZoneSelector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.web.common.locale.LocaleProvider;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

/**
 * Initialize the locale when the user session is entered. Enables client to send their timezone id through AJAX (not
 * yet implemented).
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
        return (LocaleStartup) Component.getInstance(LocaleStartup.class, ScopeType.SESSION);
    }

    @In(create = true)
    protected CoreSession documentManager;

    protected String tzId;

    protected boolean hasHandledSessionStarted = false;

    public String getTzId() {
        return tzId;
    }

    public void setTzId(String id) {
        tzId = id;
    }

    @Observer(EventNames.USER_SESSION_STARTED)
    public void handleUserSessionStarted(CoreSession session) {
        if (!hasHandledSessionStarted) {
            setupTimeZone(session);
            setupLocale(session);
            hasHandledSessionStarted = true;
        }
    }

    /**
     * Getting the timezone from the cookies and initialize Seam timezone. The nxtimezone.js contains methods to set the
     * cookie with the browser timezone.
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
        Locale locale = Framework.getService(LocaleProvider.class).getLocale(session);
        setupLocale(locale);
    }

    /**
     * @since 5.9.5
     */
    public void setupLocale(DocumentModel userProfileDoc) {
        Locale locale = Framework.getService(LocaleProvider.class).getLocale(userProfileDoc);
        setupLocale(locale);
    }

    protected void setupLocale(Locale locale) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (locale == null && ctx != null) {
            log.debug("Locale not set, falling back to request locale");
            locale = ctx.getExternalContext().getRequestLocale();
        }
        if (locale == null && ctx != null) {
            log.debug("Locale not set, falling back to default JSF locale");
            locale = ctx.getApplication().getDefaultLocale();
        }
        if (locale == null) {
            log.debug("Locale not set, falling back to default locale");
            locale = Locale.getDefault();
        }
        LocaleSelector localeSelector = LocaleSelector.instance();
        // check if locale is accepted for setup
        boolean set = false;
        if (ctx != null) {
            Locale jsfDefault = ctx.getApplication().getDefaultLocale();
            if (jsfDefault != null && jsfDefault.equals(locale)) {
                set = true;
            } else {
                Iterator<Locale> it = ctx.getApplication().getSupportedLocales();
                while (it.hasNext()) {
                    Locale current = it.next();
                    if (current.equals(locale)) {
                        set = true;
                        break;
                    }
                }
            }
        }
        if (!set) {
            if (log.isDebugEnabled()) {
                log.debug(
                        "Locale was not set to '" + locale + "' as it could not be validated as a supported language.");
            }
        } else {
            localeSelector.setLocale(locale);
            localeSelector.setCookieEnabled(true);
            localeSelector.select();
        }
    }

    @Destroy
    public void destroy() {
        hasHandledSessionStarted = false;
    }

}
