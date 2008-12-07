/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.gwt.client.ui.impl;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.TextBox;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultHeader extends Composite {

    public DefaultHeader() {
        Grid grid = new Grid(1, 3);
        grid.setCellPadding(2);
        Image img = new Image("http://google-web-toolkit-doc-1-5.googlecode.com/svn/wiki/gwt-logo.png");
        //img.setPixelSize(60, 60);
        grid.setWidget(0, 0, img);
        grid.getCellFormatter().setAlignment(0, 1, HasHorizontalAlignment.ALIGN_LEFT, HasVerticalAlignment.ALIGN_MIDDLE);
        grid.setWidget(0, 1, new HTML("<h1>Nuxeo</h1>"));
        grid.getCellFormatter().setAlignment(0, 1, HasHorizontalAlignment.ALIGN_LEFT, HasVerticalAlignment.ALIGN_BOTTOM);
        TextBox box = new TextBox();
        box.setText("Search");
        grid.setWidget(0, 2, box);
        grid.getCellFormatter().setAlignment(0, 2, HasHorizontalAlignment.ALIGN_RIGHT, HasVerticalAlignment.ALIGN_BOTTOM);
        grid.getCellFormatter().setWidth(0, 2, "100%");
        initWidget(grid);
    }

}
