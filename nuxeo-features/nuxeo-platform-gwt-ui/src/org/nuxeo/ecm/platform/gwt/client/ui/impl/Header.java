/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
