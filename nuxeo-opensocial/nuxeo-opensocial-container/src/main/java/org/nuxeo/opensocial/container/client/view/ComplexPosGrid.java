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

package org.nuxeo.opensocial.container.client.view;

/**
 * ComplexPosGrid
 *
 * @author Guillaume Cusnieux
 */
public class ComplexPosGrid extends DefaultPosGrid implements PosGrid {

  private int yPos;
  private int height;

  public ComplexPosGrid(int xPos, int width, int yPos, int height) {
    super(xPos, width);
    this.yPos = yPos;
    this.height = height;
  }

  @Override
  public boolean isCol(int[] coordinates) {
    return (coordinates[0] < (xPos + width) && coordinates[1] < (yPos + height));
  }
}
