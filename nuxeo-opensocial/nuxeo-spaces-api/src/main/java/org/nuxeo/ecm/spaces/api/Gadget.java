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

import java.util.Map;

public interface Gadget {

  /**
   * Unique identifier of a gadget
   * 
   * @return
   */
  String getId();

  /**
   * name
   * 
   * @return
   */
  String getName();

  /**
   * description
   * 
   * @return
   */
  String getDescription();

  /**
   * title
   * 
   * @return
   */
  String getTitle();

  /**
   * creator name
   * 
   * @return
   */
  String getOwner();

  /**
   * use category
   * 
   * @return
   */
  // @Deprecated
  // String getType();

  /**
   * category of a gadget
   * 
   * @return
   */
  String getCategory();

  /**
   * preferences values
   * 
   * @return
   */
  public Map<String, String> getPreferences();

  /**
   * Key corresponding to the place where the gadget will be positionned in the
   * view
   * 
   * @return
   */
  String getPlaceID();

  /**
   * Relative position in the parent container at the place id "getPlaceID()"
   * 
   * @return
   */
  int getPosition();

  /**
   * Relative height of gadget
   * 
   * @return
   */
  int getHeight();

  /**
   * Html Content of gadget
   * 
   * @return
   */
  String getHtmlContent();

  /**
   * Determines if the display state of the gadget
   * 
   * @return
   */
  boolean isCollapsed();

  public boolean isEqualTo(Gadget gadget);
}
