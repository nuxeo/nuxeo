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

package org.nuxeo.ecm.platform.gwt.client;

import org.nuxeo.ecm.platform.gwt.client.ui.ExtensionPoints;
import org.nuxeo.ecm.platform.gwt.client.ui.admin.AdministrationView;
import org.nuxeo.ecm.platform.gwt.client.ui.clipboard.ClipboardView;
import org.nuxeo.ecm.platform.gwt.client.ui.editor.DocumentEditor;
import org.nuxeo.ecm.platform.gwt.client.ui.editor.DocumentMetadataPage;
import org.nuxeo.ecm.platform.gwt.client.ui.editor.DocumentViewPage;
import org.nuxeo.ecm.platform.gwt.client.ui.editor.FolderViewPage;
import org.nuxeo.ecm.platform.gwt.client.ui.editor.HtmlView;
import org.nuxeo.ecm.platform.gwt.client.ui.editor.SmartEditorManager;
import org.nuxeo.ecm.platform.gwt.client.ui.editor.UrlView;
import org.nuxeo.ecm.platform.gwt.client.ui.impl.Footer;
import org.nuxeo.ecm.platform.gwt.client.ui.impl.Header;
import org.nuxeo.ecm.platform.gwt.client.ui.impl.SmartApplication;
import org.nuxeo.ecm.platform.gwt.client.ui.impl.ViewStack;
import org.nuxeo.ecm.platform.gwt.client.ui.navigator.NavigatorView;
import org.nuxeo.ecm.platform.gwt.client.ui.search.SearchEditor;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Bundle(DefaultBundle.class)
public interface SmartBundle extends ApplicationBundle {

    @Extension(targets=Framework.APPLICATION_XP)
    @ExtensionPoint({
        ExtensionPoints.LEFT_AREA_XP,
        ExtensionPoints.RIGHT_AREA_XP,
        ExtensionPoints.CONTENT_AREA_XP,
        ExtensionPoints.HEADER_AREA_XP,
        ExtensionPoints.FOOTER_AREA_XP})
     SmartApplication applicationWindow();

    @Extension(targets=ExtensionPoints.CONTENT_AREA_XP)
    @ExtensionPoint(ExtensionPoints.EDITORS_XP)
    SmartEditorManager editorManager();

    @Extension(targets=ExtensionPoints.LEFT_AREA_XP)
    @ExtensionPoint(ExtensionPoints.VIEWS_XP)
    ViewStack viewContainer();

//    @Extension(targets=ExtensionPoints.RIGHT_AREA_XP)
//    Right rightArea();

    @Extension(targets=ExtensionPoints.HEADER_AREA_XP)
    Header header();

    @Extension(targets=ExtensionPoints.FOOTER_AREA_XP)
    Footer footer();

    @Extension(targets=ExtensionPoints.VIEWS_XP, hint=100)
    NavigatorView navigatorView();

    @Extension(targets=ExtensionPoints.VIEWS_XP, hint=200)
    AdministrationView administration();

    @Extension(targets=ExtensionPoints.VIEWS_XP, hint=300)
    ClipboardView clipboard();

    @Extension(targets=ExtensionPoints.EDITORS_XP)
    HtmlView htmlView();

    @Extension(targets=ExtensionPoints.EDITORS_XP)
    UrlView urlView();

    @ExtensionPoint(ExtensionPoints.EDITOR_PAGES_XP)
    @Extension(targets=ExtensionPoints.EDITORS_XP)
    DocumentEditor documentEditor();

    @Extension(targets=ExtensionPoints.EDITORS_XP)
    SearchEditor searchEditor();
    
    @Extension(targets=ExtensionPoints.EDITOR_PAGES_XP)
    FolderViewPage folderViewPage();

    @Extension(targets=ExtensionPoints.EDITOR_PAGES_XP)
    DocumentViewPage documentViewPage();

    @Extension(targets=ExtensionPoints.EDITOR_PAGES_XP)
    DocumentMetadataPage documentMetadataPage();

}
