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

package org.nuxeo.opensocial.container.client.external.opensocial;

import org.nuxeo.opensocial.container.client.gadgets.AbstractGadget;
import org.nuxeo.opensocial.container.client.gadgets.facets.IsClosable;
import org.nuxeo.opensocial.container.client.gadgets.facets.IsCollapsable;
import org.nuxeo.opensocial.container.client.gadgets.facets.IsConfigurable;
import org.nuxeo.opensocial.container.client.gadgets.facets.IsMaximizable;

import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Stéphane Fourrier
 */
public class OpenSocialGadget extends AbstractGadget implements
        OpenSocialPresenter.Display {

    private Frame frame;

    public OpenSocialGadget() {
        frame = new Frame();
        frame.setWidth("100%");
        frame.getElement().getStyle().setBorderStyle(BorderStyle.NONE);
        frame.getElement().getStyle().setOverflow(Overflow.HIDDEN);

        initWidget(frame);
    }

    public void enableFacets() {
        addFacet(new IsCollapsable());
        addFacet(new IsConfigurable());
        addFacet(new IsMaximizable());
        addFacet(new IsClosable());
    }

    public void setUrl(String url) {
        frame.setUrl(url);
    }

    public void setHeight(int height) {
        frame.setHeight(height + "px");
    }

    public Widget asWidget() {
        return this;
    }

    public void startProcessing() {
    }

    public void stopProcessing() {
    }

    public String getId() {
        return frame.getElement().getAttribute("id");
    }

    public void setId(String id) {
        frame.getElement().setAttribute("id", id);
    }

    public void setName(String name) {
        frame.getElement().setAttribute("name", name);
    }
}
