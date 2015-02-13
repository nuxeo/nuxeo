/*
 * (C) Copyright 2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.rendition;

import java.util.List;

import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;

/**
 * Interface on an Object that can be used to produce {@link Rendition}
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * 
 */
public interface Renderable {

    /**
     * Returns {@link RenditionDefinition} that are available on the underlying
     * object
     * 
     * @return
     */
    List<RenditionDefinition> getAvailableRenditionDefinitions();

    /**
     * Retrieve the {@link Rendition} by it's name
     * 
     * @param name
     * @return
     * @throws RenditionException
     */
    Rendition getRenditionByName(String name) throws RenditionException;

    /**
     * Retrieve the {@link Rendition} by it's king (first match rendition is
     * returned)
     * 
     * @param name
     * @return
     * @throws RenditionException
     */
    Rendition getRenditionByKind(String name) throws RenditionException;

}
