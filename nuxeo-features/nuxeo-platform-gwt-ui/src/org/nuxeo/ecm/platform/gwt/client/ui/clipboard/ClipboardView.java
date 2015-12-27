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

package org.nuxeo.ecm.platform.gwt.client.ui.clipboard;

import org.nuxeo.ecm.platform.gwt.client.ui.SmartView;
import org.nuxeo.ecm.platform.gwt.client.ui.SmartWidget;
import org.nuxeo.ecm.platform.gwt.client.ui.old.NavigatorTree;

import com.smartgwt.client.widgets.Canvas;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ClipboardView extends SmartView {


    public ClipboardView() {
        super("clipboard");
    }


    @Override
    public String getTitle() {
        return "Clipboard";
    }

    @Override
    protected Canvas createWidget() {
        //return new ListGrid();
        Canvas c = new SmartWidget(new NavigatorTree());
        c.setWidth100();
        c.setHeight100();
        return c;
    }

}
