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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.types.localconfiguration;

/**
 * @author <a href="mailto:qlamerand@nuxeo.com">Quentin Lamerand</a>
 */
public class ContentViewConfigurationConstants {

    private ContentViewConfigurationConstants() {
        // Constants class
    }

    public static final String CONTENT_VIEW_CONFIGURATION_FACET = "ContentViewLocalConfiguration";

    public static final String CONTENT_VIEW_CONFIGURATION_CATEGORY = "content";

    public static final String CONTENT_VIEW_CONFIGURATION_NAMES_BY_TYPE = "cvconf:cvNamesByType";

    public static final String CONTENT_VIEW_CONFIGURATION_CONTENT_VIEW = "contentView";

    public static final String CONTENT_VIEW_CONFIGURATION_DOC_TYPE = "docType";

}
