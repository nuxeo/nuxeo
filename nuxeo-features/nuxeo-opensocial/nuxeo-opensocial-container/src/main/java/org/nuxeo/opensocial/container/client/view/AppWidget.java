/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stéphane Fourrier
 */

package org.nuxeo.opensocial.container.client.view;

import org.nuxeo.opensocial.container.client.presenter.AppPresenter;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import net.customware.gwt.presenter.client.widget.WidgetDisplay;

/**
 * @author Stéphane Fourrier
 */
public class AppWidget extends Composite implements AppPresenter.Display {
    private final AbsolutePanel panel;

    public AppWidget() {
        panel = new AbsolutePanel();
        panel.setWidth("100%");
        initWidget(panel);
    }

    public Widget asWidget() {
        return this;
    }

    public void addContent(WidgetDisplay display) {
        panel.add(display.asWidget());
    }

    public void startProcessing() {
    }

    public void stopProcessing() {
    }
}
