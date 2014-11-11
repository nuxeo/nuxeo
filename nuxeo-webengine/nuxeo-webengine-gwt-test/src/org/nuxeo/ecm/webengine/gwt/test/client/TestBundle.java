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

package org.nuxeo.ecm.webengine.gwt.test.client;

import org.nuxeo.ecm.webengine.gwt.client.ApplicationBundle;
import org.nuxeo.ecm.webengine.gwt.client.Bundle;
import org.nuxeo.ecm.webengine.gwt.client.Extension;
import org.nuxeo.ecm.webengine.gwt.client.ui.ExtensionPoints;
import org.nuxeo.ecm.webengine.gwt.client.ui.impl.DefaultApplicationBundle;
import org.nuxeo.ecm.webengine.gwt.client.ui.navigator.NavigatorView;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Bundle(DefaultApplicationBundle.class)
public interface TestBundle extends ApplicationBundle {


    @Extension(targets=ExtensionPoints.VIEWS_XP, hint=1000)
    TestView testView();

    @Extension(targets=ExtensionPoints.VIEWS_XP)
    TestExtension testExtension();

    @Extension(targets=ExtensionPoints.VIEWS_XP, hint=100)
    NavigatorView navigatorView();

}
