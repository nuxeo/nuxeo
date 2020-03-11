/*
 * (C) Copyright 2011-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *   Nuxeo - initial API and implementation
 */
package org.nuxeo.theme.localconfiguration;

import org.nuxeo.ecm.core.api.localconfiguration.LocalConfiguration;

/**
 * Local configuration class to handle configuration of theme.
 *
 * @author <a href="mailto:qlamerand@nuxeo.com">Quentin Lamerand</a>
 */
public interface LocalThemeConfig extends LocalConfiguration<LocalThemeConfig> {

    String OLD_THEME_CONFIGURATION_PROPERTY = "theme.useOldLocalConfiguration";

    /**
     * Returns the flavor (collection) to use for current page
     *
     * @since 5.5
     */
    String getFlavor();

}
