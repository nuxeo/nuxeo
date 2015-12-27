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

package org.nuxeo.ecm.platform.gwt.client.ui.editor;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.platform.gwt.client.Extensible;
import org.nuxeo.ecm.platform.gwt.client.model.Document;
import org.nuxeo.ecm.platform.gwt.client.ui.ExtensionPoints;
import org.nuxeo.ecm.platform.gwt.client.ui.View;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DocumentEditor implements Editor, Extensible {

    protected List<EditorPage> pageViews = new ArrayList<EditorPage>();

    public boolean acceptInput(Object input) {
        return input instanceof Document;
    }

    public View getView() {
        MultiPageDocView mpview = new MultiPageDocView();
        for (EditorPage page : pageViews) {
            mpview.addPage(page.getName(), page.getView());
        }
        return mpview;
    }

    public void registerExtension(String target, Object extension) {
        if (ExtensionPoints.EDITOR_PAGES_XP.equals(target)) {
            pageViews.add((EditorPage)extension);
        }
    }

}
