/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: LayoutTagLibrary.java 28493 2008-01-04 19:51:30Z sfermigier $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets.library;

import org.nuxeo.ecm.platform.forms.layout.facelets.DocumentLayoutTagHandler;
import org.nuxeo.ecm.platform.forms.layout.facelets.LayoutRowTagHandler;
import org.nuxeo.ecm.platform.forms.layout.facelets.LayoutRowWidgetTagHandler;
import org.nuxeo.ecm.platform.forms.layout.facelets.LayoutTagHandler;
import org.nuxeo.ecm.platform.forms.layout.facelets.SubWidgetTagHandler;
import org.nuxeo.ecm.platform.forms.layout.facelets.WidgetTagHandler;
import org.nuxeo.ecm.platform.forms.layout.facelets.WidgetTypeTagHandler;

import com.sun.facelets.tag.AbstractTagLibrary;

/**
 * Layout tag library
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class LayoutTagLibrary extends AbstractTagLibrary {

    public final static String Namespace = "http://nuxeo.org/nxforms/layout";

    public final static LayoutTagLibrary Instance = new LayoutTagLibrary();

    public LayoutTagLibrary() {
        super(Namespace);

        addTagHandler("widgetType", WidgetTypeTagHandler.class);
        addTagHandler("widget", WidgetTagHandler.class);
        addTagHandler("layout", LayoutTagHandler.class);
        addTagHandler("layoutRow", LayoutRowTagHandler.class);
        addTagHandler("layoutRowWidget", LayoutRowWidgetTagHandler.class);
        addTagHandler("subWidget", SubWidgetTagHandler.class);
        addTagHandler("documentLayout", DocumentLayoutTagHandler.class);

    }

}
