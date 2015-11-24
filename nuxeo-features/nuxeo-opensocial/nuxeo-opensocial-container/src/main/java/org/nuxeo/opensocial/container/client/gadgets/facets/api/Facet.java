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

package org.nuxeo.opensocial.container.client.gadgets.facets.api;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Stéphane Fourrier
 */
public class Facet extends Composite implements HasClickHandlers {
    private static final String CSS_OVER = "over";

    private FocusPanel facet;

    private String title1 = null;

    private String title2 = null;

    private String cssClassForState1 = null;

    private String cssClassForState2 = null;

    private GwtEvent<?> eventToFireOnState1 = null;

    private GwtEvent<?> eventToFireOnState2 = null;

    private boolean isInState1 = true;

    public Facet(String title, String cssClass, GwtEvent<?> eventToFireOnState1) {
        title1 = title;
        cssClassForState1 = cssClass;

        facet = new FocusPanel();
        facet.setStyleName("facet");
        facet.addStyleName(cssClass);
        facet.setTitle(title);

        facet.addMouseOverHandler(new MouseOverHandler() {
            public void onMouseOver(MouseOverEvent event) {
                facet.addStyleName(CSS_OVER);
            }
        });

        facet.addMouseOutHandler(new MouseOutHandler() {
            public void onMouseOut(MouseOutEvent event) {
                facet.removeStyleName(CSS_OVER);
            }
        });

        facet.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                facet.removeStyleName(CSS_OVER);
            }
        });

        initWidget(facet);

        this.eventToFireOnState1 = eventToFireOnState1;
    }

    public Facet(String title1, String cssClassForState1, GwtEvent<?> eventToFireOnState1,
            String title2, String cssClassForState2, GwtEvent<?> eventToFireOnState2) {
        this(title1, cssClassForState1, eventToFireOnState1);

        this.title2 = title2;
        this.cssClassForState2 = cssClassForState2;
        this.eventToFireOnState2 = eventToFireOnState2;
    }

    public void changeState() {
        if (cssClassForState2 != null) {
            if (isInFirstState()) {
                facet.removeStyleName(cssClassForState1);
                facet.addStyleName(cssClassForState2);
                facet.setTitle(title2);
                isInState1 = false;
            } else {
                facet.removeStyleName(cssClassForState2);
                facet.addStyleName(cssClassForState1);
                facet.setTitle(title1);
                isInState1 = true;
            }
        }
    }

    public void enable() {
        this.setVisible(true);
    }

    public void disable() {
        this.setVisible(false);
    }

    public Widget asWidget() {
        return this;
    }

    public HandlerRegistration addClickHandler(ClickHandler handler) {
        return facet.addClickHandler(handler);
    }

    public GwtEvent<?> getEventToFire() {
        if (isInFirstState())
            return eventToFireOnState1;
        else
            return eventToFireOnState2;
    }

    public boolean isInFirstState() {
        return isInState1;
    }
}
