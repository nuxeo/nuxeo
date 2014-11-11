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


/**
 * An item container which is showing only one item at a time.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class StackedViewContainer extends ViewContainer {


    public StackedViewContainer(String name) {
        super(name);
    }

    public abstract int getSelectedIndex();


    public abstract void selectView(int index);

    public View getSelectedView() {
        int i = getSelectedIndex();
        if (i > -1) {
            return (View)getPanel().getWidget(i);
        }
        return null;
    }

    public void selectView(View view) {
        int i = indexOf(view);
        if (i > -1) {
            selectView(i);
        }
    }

    public void selectView(String name) {
        int i = indexOf(name);
        if (i > -1) {
            selectView(i);
        }
    }

}
