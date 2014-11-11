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

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.localconfiguration.LocalConfiguration;

/**
 * Local configuration class to handle configuration of DocumentContentView.
 *
 * @author <a href="mailto:qlamerand@nuxeo.com">Quentin Lamerand</a>
 */
public interface ContentViewConfiguration extends LocalConfiguration<ContentViewConfiguration> {

    /**
     * Returns the ContentView name for a specified document type if any is configured.
     * Else, returns null.
     */
    List<String> getContentViewsForType(String docType);

    /**
     * Returns the ContentView names for all the configured types.
     * Used for merging configurations.
     */
    Map<String, List<String>> getTypeToContentViewNames();

}
