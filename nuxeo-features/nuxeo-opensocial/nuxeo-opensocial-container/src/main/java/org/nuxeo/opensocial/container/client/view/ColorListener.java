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

import com.gwtext.client.widgets.ColorPalette;
import com.gwtext.client.widgets.event.ColorPaletteListenerAdapter;
import com.gwtext.client.widgets.form.Field;

/**
 * ColorListener
 *
 * @author Guillaume Cusnieux
 */
public class ColorListener extends ColorPaletteListenerAdapter {

  private Field input;
  private GadgetPortlet portlet;
  private String name;

  public ColorListener(GadgetPortlet gp, String name, Field input) {
    this.portlet = gp;
    this.name = name;
    this.input = input;
  }

  @Override
  public void onSelect(ColorPalette colorPalette, String color) {
    super.onSelect(colorPalette, color);
    this.input.setValue(color);
    portlet.renderPreference(this.name, color);
  }

}
