/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.theme.styling.service;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.web.resources.api.service.WebResourceManager;
import org.nuxeo.theme.styling.service.descriptors.FlavorDescriptor;
import org.nuxeo.theme.styling.service.descriptors.LogoDescriptor;
import org.nuxeo.theme.styling.service.descriptors.PageDescriptor;

/**
 * Service handling the mapping between a page and its resources and flavors.
 * <p>
 * Registers some contributions to the {@link WebResourceManager} for compatibility.
 *
 * @see FlavorDescriptor
 * @since 5.5
 */
public interface ThemeStylingService {

    String FLAVOR_MARKER = "__FLAVOR__";

    String PAGE_STYLE_CLASS_NAME_PREFIX = " CSS";

    String PAGE_STYLE_NAME_SUFFIX = " Page Styles";

    public enum PRESET_CATEGORY {
        background, border, color, font;
    }

    /**
     * Returns the default flavor for a given theme page
     */
    String getDefaultFlavorName(String themePage);

    /**
     * Returns the flavor names for a given theme page
     */
    List<String> getFlavorNames(String themePage);

    /**
     * Returns the flavors for a given theme page
     */
    List<FlavorDescriptor> getFlavors(String themePage);

    /**
     * Returns the flavor for given name, or null if not found.
     * <p>
     * If not defined on the local flavor, flavor attributes will be resolved from the extended flavor if any.
     * </p>
     */
    FlavorDescriptor getFlavor(String flavorName);

    /**
     * Returns the map of variable replacements for given flavor.
     * <p>
     * Returns an empty map if flavor is not resolved.
     *
     * @since 7.3
     */
    Map<String, String> getPresetVariables(String flavorName);

    /**
     * Returns the logo configured for given flavor name, and fallbacks on the extends flavor logo if not set.
     */
    LogoDescriptor getLogo(String flavor);

    /**
     * Returns the page for given name.
     * <p>
     * Resources and bundles declared for all pages will also be attached to returned page.
     *
     * @since 7.4
     */
    PageDescriptor getPage(String name);

    /**
     * Rerurns all pages declared on the service, except the global one named "*".
     * <p>
     * Resources and bundles declared for all pages will also be attached to returned pages.
     *
     * @since 7.10
     **/
    List<PageDescriptor> getPages();

    /**
     * Returns the negotiated String value for given target variable.
     * <p>
     * Context can be dependent on the target variable, depending on how this method is called/used and corresponding
     * negotiator implementations.
     *
     * @since 7.4
     */
    String negotiate(String target, Object context);

}
