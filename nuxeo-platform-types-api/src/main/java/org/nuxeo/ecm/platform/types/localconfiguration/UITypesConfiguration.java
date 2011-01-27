/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
import org.nuxeo.ecm.platform.types.SubType;

/**
 * Local configuration class to handle configuration of UI Types.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public interface UITypesConfiguration extends LocalConfiguration<UITypesConfiguration> {

    /**
     * Returns the configured allowed types.
     */
    List<String> getAllowedTypes();

    /**
     * Returns the configured denied types.
     */
    List<String> getDeniedTypes();

    /**
     * Returns {@code true} if all the types are denied, {@code false} otherwise.
     */
    boolean denyAllTypes();

    /**
     * Filter the {@code allowedSubTypes} according to this object configuration.
     */
    Map<String,SubType> filterSubTypes(Map<String,SubType> allowedSubTypes);
}
