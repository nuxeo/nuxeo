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

package org.nuxeo.ecm.core.api.localconfiguration;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Service handling {@code LocalConfiguration} classes.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.1
 */
public interface LocalConfigurationService {

    /**
     * Returns the first {@code LocalConfiguration} accessible from the
     * {@code currentDoc}, {@code null} otherwise.
     * <p>
     * Find the first parent of the {@code currentDoc} having the given
     * {@code configurationFacet}, if any, and adapt it on the
     * {@code configurationClass}.
     */
    public <T extends LocalConfiguration> T getConfiguration(
            Class<T> configurationClass, String configurationFacet,
            DocumentModel currentDoc);

}
