/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.theme.styling.service;

import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;
import org.nuxeo.theme.resources.ResourceManager;
import org.nuxeo.theme.services.ThemeService;
import org.nuxeo.theme.themes.ThemeManager;

/**
 * Event listener that triggers {@link ThemeStylingService} methods to impact
 * the standard {@link ThemeService} registries
 *
 * @since 5.5
 */
public class ThemeStylingNotifier implements EventListener {

    private static final Log log = LogFactory.getLog(ThemeStylingNotifier.class);

    @Override
    public boolean aboutToHandleEvent(Event event) {
        return false;
    }

    @Override
    public void handleEvent(Event event) {
        if (!ThemeManager.THEME_TOPIC.equals(event.getTopic())) {
            return;
        }
        if (ThemeManager.THEME_REGISTERED_EVENT_ID.equals(event.getId())) {
            try {
                ThemeStylingService service = Framework.getService(ThemeStylingService.class);
                service.themeRegistered((String) event.getData());
            } catch (Exception e) {
                log.error(e, e);
            }
        }
        if (ResourceManager.GLOBAL_RESOURCES_REGISTERED_EVENT.equals(event.getId())) {
            try {
                ThemeStylingService service = Framework.getService(ThemeStylingService.class);
                service.themeGlobalResourcesRegistered((URL) event.getData());
            } catch (Exception e) {
                log.error(e, e);
            }
        }
    }

}
