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
