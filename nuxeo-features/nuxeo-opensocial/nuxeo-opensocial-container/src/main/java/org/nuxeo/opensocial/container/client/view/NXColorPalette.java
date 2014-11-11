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
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.opensocial.container.client.view;

import com.gwtext.client.widgets.ColorPalette;

public class NXColorPalette extends ColorPalette {

    public NXColorPalette() {
        super();
        overrideHandleClick();
    }

    @Override
    public void select(String color) {
        super.select(color == null ? "none" : color);
    };

    public static native void overrideHandleClick()
    /*-{
      $wnd.Ext.override($wnd.Ext.ColorPalette, {
        handleClick : function(e, t){
          e.preventDefault();
          if(!this.disabled){
            var colors = t.className.match(/(?:^|\s)color-(.{6})(?:\s|$)/);
            var c = "none";
            if(colors!=null)
              c = colors[1].toUpperCase();
            this.select(c);
          }
      }
      });

     }-*/;

}
