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
