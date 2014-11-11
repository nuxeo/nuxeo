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

package org.nuxeo.ecm.webengine.gwt.client.ui;

import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.StackPanel;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ViewStack extends StackedViewContainer {

    public ViewStack(String name) {
        super(name);
    }

    @Override
    protected ComplexPanel createPanel() {
        StackPanel stackPanel = new StackPanel();
        stackPanel.ensureDebugId("viewStackPanel");
        stackPanel.setSize("100%", "100%");
        return stackPanel;
    }

    public StackPanel getStackPanel() {
        return (StackPanel)getWidget();
    }

    @Override
    public int getSelectedIndex() {
        return getStackPanel().getSelectedIndex();
    }

    @Override
    public void selectView(int index) {
        getStackPanel().showStack(index);
    }

    @Override
    public void add(View view) {
        getStackPanel().add(view, view.getHeader(), true);
    }

    @Override
    public void insert(View view, int beforeIndex) {
        StackPanel panel = getStackPanel();
        panel.insert(view, beforeIndex);
        panel.setStackText(beforeIndex, view.getHeader(), true);
    }

    @Override
    public void refresh() {
        View view = getSelectedView();
        if (view != null) {
            view.refresh();
        }
    }

}
