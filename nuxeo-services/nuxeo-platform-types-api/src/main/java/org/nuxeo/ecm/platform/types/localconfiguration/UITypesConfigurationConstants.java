/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.types.localconfiguration;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class UITypesConfigurationConstants {

    private UITypesConfigurationConstants() {
        // Constants class
    }

    public static final String UI_TYPES_CONFIGURATION_FACET = "UITypesLocalConfiguration";

    public static final String UI_TYPES_CONFIGURATION_ALLOWED_TYPES_PROPERTY = "uitypesconf:allowedTypes";

    public static final String UI_TYPES_CONFIGURATION_DENIED_TYPES_PROPERTY = "uitypesconf:deniedTypes";

    public static final String UI_TYPES_CONFIGURATION_DENY_ALL_TYPES_PROPERTY = "uitypesconf:denyAllTypes";

    public static final String UI_TYPES_CONFIGURATION_DEFAULT_TYPE = "uitypesconf:defaultType";

    public static final String UI_TYPES_DEFAULT_TYPE = "File";

    public static final String UI_TYPES_DEFAULT_NEEDED_SCHEMA = "file";

}
