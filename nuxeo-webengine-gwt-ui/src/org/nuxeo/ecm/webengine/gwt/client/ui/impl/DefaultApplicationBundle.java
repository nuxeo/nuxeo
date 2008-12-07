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

import org.nuxeo.ecm.webengine.gwt.client.ApplicationBundle;
import org.nuxeo.ecm.webengine.gwt.client.Bundle;
import org.nuxeo.ecm.webengine.gwt.client.Extension;
import org.nuxeo.ecm.webengine.gwt.client.ExtensionPoint;
import org.nuxeo.ecm.webengine.gwt.client.Framework;
import org.nuxeo.ecm.webengine.gwt.client.ui.ExtensionPoints;
import org.nuxeo.ecm.webengine.gwt.client.ui.login.LoginView;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Bundle
public interface DefaultApplicationBundle extends ApplicationBundle {

    @Extension(targets=Framework.APPLICATION_XP)
    @ExtensionPoint({
        ExtensionPoints.VIEW_CONTAINER_XP,
        ExtensionPoints.EDITOR_CONTAINER_XP,
        ExtensionPoints.HEADER_CONTAINER_XP,
        ExtensionPoints.FOOTER_CONTAINER_XP})
     ApplicationImpl applicationWindow();

    @Extension(targets=ExtensionPoints.EDITOR_CONTAINER_XP)
    @ExtensionPoint(ExtensionPoints.EDITORS_XP)
    EditorContainerImpl editorContainer();

    @Extension(targets=ExtensionPoints.VIEW_CONTAINER_XP)
    @ExtensionPoint(ExtensionPoints.VIEWS_XP)
    ViewContainerImpl viewContainer();

    @Extension(targets=ExtensionPoints.VIEWS_XP, hint=300)
    LoginView loginView();

}
