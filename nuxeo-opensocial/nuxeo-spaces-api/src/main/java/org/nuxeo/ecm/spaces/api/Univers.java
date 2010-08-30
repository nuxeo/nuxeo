/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *
 * Contributors:
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.ecm.spaces.api;

import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;

/**
 * Global container corresponding to a site . A univers can contain
 * <code>Space</code> elements . <code>Space</code> elements contained in this
 * universe are retrieved via the SpaceManager framework service : <br/>
 * <br/>
 * SpaceManager service = Framework.getService(SpaceManager.class)<br/>
 * List&lt;Space&gt; spaces = service.getSpacesForUnivers(univers,coreSession);
 */
public interface Univers {

    /**
     * UID.
     *
     * @return a unique identifier for a given instance of Univers
     *         implementation
     */
    String getId();

    /**
     * Universe name.
     *
     * @return the name of this universe
     */
    String getName();

    /**
     * Universe title.
     *
     * @return the title of this universe
     */
    String getTitle();

    /**
     * Universe description.
     *
     * @return the description of this universe
     */
    String getDescription();

    /**
     * For comparison.
     */
    boolean isEqualTo(Univers univers);

    List<Space> getSpaces(CoreSession session) throws SpaceException;

  /**
   * @param session
   * @return list of {@link SpaceProvider}
   */
  List<SpaceProvider> getSpaceProviders(CoreSession session) throws SpaceException;

}
