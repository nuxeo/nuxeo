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

import java.util.Calendar;
import java.util.List;

/**
 * Gadget container corresponding to a sub-part of an universe . A
 * <code>Space</code> can contain <code>Gadget</code> elements .
 * <code>Gadget</code> elements contained in this space are retrieved via the
 * SpaceManager framework service : <br/>
 * <br/>
 * SpaceManager service = Framework.getService(SpaceManager.class)<br/>
 * List&lt;Space&gt; gadgets = service.getGadgetsForSpace(space,coreSession);
 */
public interface Space {

  /**
   * Unique identifier of a space instance
   * 
   * @return
   */
  String getId();

  /**
   * Name of the space
   * 
   * @return
   */
  String getName();

  /**
   * Space theme
   * 
   * @return
   */
  String getTheme();

  /**
   * Title of the space
   * 
   * @return
   */
  String getTitle();

  /**
   * description of the space
   * 
   * @return
   */
  String getDescription();

  /**
   * A key for displaying elements in this space
   * 
   * @return
   */
  String getLayout();

  /**
   * Family/category of this space
   * 
   * @return
   */
  String getCategory();

  /**
   * Name of the creator of this space
   * 
   * @return
   */
  String getOwner();

  /**
   * Date publication of this space
   * 
   * @return
   */
  Calendar getDatePublication();

  /**
   * versionnable property
   * 
   * @return
   */
  boolean isVersionnable();

  /**
   * current version property
   * 
   * @return
   */
  boolean isCurrentVersion();

  List<Space> getVersions();

  /**
   * for comparison
   * 
   * @param space
   * @return
   */
  boolean isEqualTo(Space space);

}
