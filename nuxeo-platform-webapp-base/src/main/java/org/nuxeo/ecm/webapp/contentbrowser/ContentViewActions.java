/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.webapp.contentbrowser;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.ui.web.contentview.ContentView;

/**
 * @author Anahide Tchertchian
 */
public interface ContentViewActions {

    /**
     * Returns content view with given name, or null if no content view with
     * this name is found.
     * <p>
     * The content view is put in a cache map so that it's not rebuilt at each
     * call. It is rebuilt when its cache key changes (if defined).
     *
     * @throws ClientException
     */
    ContentView getContentView(String name) throws ClientException;

    /**
     * Removes the content view with this name from cache so that it's rebuilt
     * next time it's called.
     */
    void refresh(String name);

    /**
     * Refreshes all content views that have declared the given seam event name
     * as a refresh event in their XML configuration.
     */
    void refreshOnSeamEvents(String seamEventName);

}
