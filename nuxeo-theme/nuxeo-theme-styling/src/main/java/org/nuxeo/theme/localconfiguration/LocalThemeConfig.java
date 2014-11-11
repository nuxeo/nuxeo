/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.theme.localconfiguration;

import org.nuxeo.ecm.core.api.localconfiguration.LocalConfiguration;

/**
 * Local configuration class to handle configuration of theme.
 *
 * @author <a href="mailto:qlamerand@nuxeo.com">Quentin Lamerand</a>
 */
public interface LocalThemeConfig extends LocalConfiguration<LocalThemeConfig> {

    public static final String OLD_THEME_CONFIGURATION_PROPERTY = "theme.useOldLocalConfiguration";

    /**
     * Returns the configured theme.
     */
    @Deprecated
    String getTheme();

    /**
     * Returns the configured page for the selected theme.
     */
    @Deprecated
    String getPage();

    /**
     * Returns the configured perspective used in the {@code LocalPerspective}
     * negociation scheme.
     */
    @Deprecated
    String getPerspective();

    /**
     * Returns the configured engine.
     */
    @Deprecated
    String getEngine();

    /**
     * Returns the configured mode.
     */
    @Deprecated
    String getMode();

    /**
     * Returns the page path used in the {@code LocalTheme} negociation scheme
     */
    @Deprecated
    String computePagePath();

    /**
     * Returns the flavor (collection) to use for current page
     *
     * @since 5.5
     */
    String getFlavor();

}
