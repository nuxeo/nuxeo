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
import com.google.gwt.user.client.ui.Widget;

/**
 * A view container is a composite that wraps a complex panel. It is used to
 * manage views
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class ViewContainer extends View {

    public ViewContainer(String name) {
        super(name);
    }

    @Override
    protected final Widget createContent() {
        return createPanel();
    }

    protected abstract ComplexPanel createPanel();

    public abstract void add(View view);

    public abstract void insert(View view, int beforeIndex);

    public boolean insertBefore(View view, String name) {
        int index = indexOf(name);
        if (index > -1) {
            insert(view, index);
            return true;
        }
        return false;
    }

    public ComplexPanel getPanel() {
        return (ComplexPanel) getWidget();
    }

    public int indexOf(View view) {
        return getPanel().getWidgetIndex(view);
    }

    public int indexOf(String name) {
        ComplexPanel panel = getPanel();
        int cnt = panel.getWidgetCount();
        for (int i = 0; i < cnt; i++) {
            View view = (View) panel.getWidget(i);
            if (name.equals(view.getName())) {
                return i;
            }
        }
        return -1;
    }

    public View findView(String name) {
        ComplexPanel panel = getPanel();
        int cnt = panel.getWidgetCount();
        for (int i = 0; i < cnt; i++) {
            View view = (View) panel.getWidget(i);
            if (name.equals(view.getName())) {
                return view;
            }
        }
        return null;
    }

    public View getView(int index) {
        return (View) getPanel().getWidget(index);
    }

    public int getViewsCount() {
        return getPanel().getWidgetCount();
    }

    public View[] getViews() {
        ComplexPanel panel = getPanel();
        View[] views = new View[panel.getWidgetCount()];
        for (int i = 0; i < views.length; i++) {
            views[i] = (View) panel.getWidget(i);
        }
        return views;
    }

    public void refresh() {
        ComplexPanel panel = getPanel();
        for (int i = 0, cnt = panel.getWidgetCount(); i < cnt; i++) {
            Widget w = panel.getWidget(i);
            if (w.isVisible()) {
                ((View) w).refresh();
            }
        }
    }

}
