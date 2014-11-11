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

package org.nuxeo.ecm.platform.gwt.client.ui.impl;

import org.nuxeo.ecm.platform.gwt.client.Framework;
import org.nuxeo.ecm.platform.gwt.client.ui.SmartView;
import org.nuxeo.ecm.platform.gwt.client.ui.widgets.Navbar;
import org.nuxeo.ecm.platform.gwt.client.ui.widgets.SearchBar;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Header extends SmartView {

    public Header() {
        super ("header");
    }
    

    protected Canvas createWidget() {

        HLayout header = new HLayout();
        Canvas canvas = new Img(Framework.getSkinPath("/images/logo.gif"));
        header.addMember(canvas);
        
        VLayout rightHeader = new VLayout();
        Navbar navbar = new Navbar();
        rightHeader.addMember(navbar);
        
        rightHeader.addMember(new SearchBar());
        
        header.addMember(rightHeader);
        return header;        
    }
    
}
