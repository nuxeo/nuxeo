/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     stan
 */

package org.nuxeo.ecm.webapp.webcontainer;

import java.io.Serializable;

import org.nuxeo.theme.styling.service.descriptors.FlavorDescriptor;
import org.nuxeo.theme.styling.service.descriptors.LogoDescriptor;

public interface ThemeActions extends Serializable {

    /**
     * Returns negotiated default theme to handle print action.
     */
    String getDefaultTheme();

    /**
     * Returns the logo configured for negotiated flavor.
     *
     * @since 5.5.
     */
    LogoDescriptor getLogo();

    /**
     * Returns the logo configured for given flavor.
     *
     * @since 5.5.
     */
    LogoDescriptor getLogo(String flavorName);

    /**
     * Returns flavor with given name.
     */
    FlavorDescriptor getFlavor(String flavorName);

    /**
     * Returns current negotiated flavor.
     * <p>
     * Assumes current page has already been resolved for this flavor negotiation.
     *
     * @since 7.4
     */
    String getCurrentFlavor();

    /**
     * Returns current negotiated flavor for given page, which is set as current page.
     *
     * @since 7.4
     */
    String getCurrentFlavor(String pageName);

    /**
     * Returns current negotiated page.
     *
     * @since 7.4
     */
    String getCurrentPage();

    /**
     * Sets the current page.
     *
     * @since 7.4
     */
    void setCurrentPage(String pageName);

}
