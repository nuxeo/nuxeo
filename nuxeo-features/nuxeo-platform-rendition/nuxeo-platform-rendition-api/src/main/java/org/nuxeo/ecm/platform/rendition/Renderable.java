/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.rendition;

import java.util.List;

import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;

/**
 * Interface on an Object that can be used to produce {@link Rendition}
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public interface Renderable {

    /**
     * Returns {@link RenditionDefinition} that are available on the underlying object
     *
     * @return
     */
    List<RenditionDefinition> getAvailableRenditionDefinitions();

    /**
     * Retrieve the {@link Rendition} by it's name
     *
     * @param name
     * @return
     */
    Rendition getRenditionByName(String name);

    /**
     * Retrieve the {@link Rendition} by it's king (first match rendition is returned)
     *
     * @param name
     * @return
     */
    Rendition getRenditionByKind(String name);

}
