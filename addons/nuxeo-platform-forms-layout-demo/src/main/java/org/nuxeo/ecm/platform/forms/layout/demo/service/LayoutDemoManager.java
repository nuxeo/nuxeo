/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.demo.service;

import java.io.Serializable;
import java.util.List;

/**
 * Manager for demo objects
 *
 * @author Anahide Tchertchian
 */
public interface LayoutDemoManager extends Serializable {

    static final String APPLICATION_PATH = "layoutDemo/";

    DemoWidgetType getWidgetType(String widgetTypeName);

    DemoWidgetType getWidgetTypeByViewId(String viewId);

    List<DemoWidgetType> getWidgetTypes(String category);

}
