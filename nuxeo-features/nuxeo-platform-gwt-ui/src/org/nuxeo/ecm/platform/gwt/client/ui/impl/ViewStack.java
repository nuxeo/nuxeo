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

import org.nuxeo.ecm.platform.gwt.client.Extensible;
import org.nuxeo.ecm.platform.gwt.client.ui.ExtensionPoints;
import org.nuxeo.ecm.platform.gwt.client.ui.SmartView;
import org.nuxeo.ecm.platform.gwt.client.ui.View;
import org.nuxeo.ecm.platform.gwt.client.ui.view.DefaultViewManager;

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.widgets.layout.SectionStack;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ViewStack extends SmartView implements Extensible {

    protected DefaultViewManager mgr;

    public ViewStack() {
        super("views");
        mgr = new DefaultViewManager(new StackContainer());
    }

    @Override
    protected void inputChanged() {
        mgr.open(input);
    }

    public SectionStack createWidget() {
        setInput(null); // force sections creation
        return ((StackContainer)mgr.getContainer()).getWidget();
    }

    public void registerExtension(String target, Object extension) {
        if (ExtensionPoints.VIEWS_XP.equals(target)) {
            try {
                View v = (View)extension;
                mgr.addView(v.getName(), v);
            } catch (ClassCastException e) {
                GWT.log("Invalid contribution to extension point: "+ExtensionPoints.VIEWS_XP, e);
            }
        }
    }

}
