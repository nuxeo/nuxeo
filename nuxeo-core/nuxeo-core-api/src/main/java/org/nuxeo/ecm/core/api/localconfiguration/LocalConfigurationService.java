/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.core.api.localconfiguration;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Service handling {@code LocalConfiguration} classes.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.2
 */
public interface LocalConfigurationService {

    /**
     * Returns the first {@code LocalConfiguration} accessible from the {@code currentDoc}, {@code null} otherwise.
     * <p>
     * Find the first parent of the {@code currentDoc} having the given {@code configurationFacet}, if any, and adapt it
     * on the {@code configurationClass}.
     */
    <T extends LocalConfiguration> T getConfiguration(Class<T> configurationClass, String configurationFacet,
            DocumentModel currentDoc);

}
