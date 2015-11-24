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

import org.nuxeo.opensocial.container.client.presenter.CustomWebContentPresenter;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Stéphane Fourrier
 */
public class CustomWebContentWidget extends Composite implements
        CustomWebContentPresenter.Display {

    private AbsolutePanel webContentPanel;

    public CustomWebContentWidget() {
        webContentPanel = new AbsolutePanel();

        initWidget(webContentPanel);
    }

    public void resize(int height) {
        webContentPanel.setHeight(height + "px");
    }

    public void clean() {
        this.removeFromParent();
    }

    public String getId() {
        return this.getElement().getAttribute("id");
    }

    public String getParentId() {
        return this.getElement().getParentElement().getAttribute("id");
    }

    public void setId(String id) {
        this.getElement().setAttribute("id", id);
    }

    public void addContent(Widget widget) {
        webContentPanel.add(widget);
    }

    public Widget asWidget() {
        return this;
    }

    public void startProcessing() {
    }

    public void stopProcessing() {
    }
}
