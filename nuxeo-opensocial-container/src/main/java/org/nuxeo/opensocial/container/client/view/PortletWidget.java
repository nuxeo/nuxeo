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

import org.nuxeo.opensocial.container.client.presenter.PortletPresenter;
import org.nuxeo.opensocial.container.client.ui.enume.ColorsEnum;
import org.nuxeo.opensocial.container.client.utils.ElementUtils;

import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Stéphane Fourrier
 */
public class PortletWidget extends Composite implements
        PortletPresenter.Display {

    private static final String TITLE_COLOR_PREFIX_CSS = "title-color-";

    private static final String HIDEICON_CLASS = "hideicon";

    private static final String NOSHADOW_CLASS = "noshadow";

    private FlowPanel portletPanel;

    private SimplePanel headerPanel;

    private HorizontalPanel headerContent;

    private FlowPanel contentPanel;

    private HTML title;

    public PortletWidget() {
        portletPanel = new FlowPanel();
        portletPanel.addStyleName("widget");
        portletPanel.setWidth("100%");

        headerPanel = new SimplePanel();
        headerPanel.addStyleName("header");
        portletPanel.add(headerPanel);

        headerContent = new HorizontalPanel();
        headerContent.addStyleName("header-content");
        headerPanel.add(headerContent);

        title = new HTML();
        title.setStyleName("title");
        headerContent.add(title);
        headerContent.setCellWidth(title, "100%");

        contentPanel = new FlowPanel();
        contentPanel.addStyleName("content");
        portletPanel.add(contentPanel);

        initWidget(portletPanel);
    }

    public boolean isCollapsed() {
        return contentPanel.isVisible();
    }

    public void showContent() {
        contentPanel.setVisible(true);
    }

    public void hideContent() {
        contentPanel.setVisible(false);
    }

    public void clean() {
        this.removeFromParent();
    }

    public Widget getHeader() {
        return title;
    }

    public void addTool(Widget widget) {
        headerContent.add(widget);
        headerContent.setCellVerticalAlignment(widget,
                HasVerticalAlignment.ALIGN_TOP);
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
        contentPanel.add(widget);
    }

    public void setTitle(String title) {
        this.title.setHTML(title);
    }

    public void setTitleColor(String color) {
        ElementUtils.removeStyle(title.getElement(), TITLE_COLOR_PREFIX_CSS);
        if (color != null) {
            title.addStyleName(TITLE_COLOR_PREFIX_CSS + color);
        } else {
            title.addStyleName(TITLE_COLOR_PREFIX_CSS
                    + ColorsEnum.NONE.getCssColor());
        }
    }

    public void setIcon(String icon) {
        headerContent.addStyleName(icon);
    }

    public Widget asWidget() {
        return this;
    }

    public void startProcessing() {
    }

    public void stopProcessing() {
    }

    public HandlerRegistration addMouseDownHandler(MouseDownHandler handler) {
        return addDomHandler(handler, MouseDownEvent.getType());
    }

    public HandlerRegistration addMouseUpHandler(MouseUpHandler handler) {
        return addDomHandler(handler, MouseUpEvent.getType());
    }

    public HandlerRegistration addMouseMoveHandler(MouseMoveHandler handler) {
        return addDomHandler(handler, MouseMoveEvent.getType());
    }

    public HandlerRegistration addMouseOutHandler(MouseOutHandler handler) {
        return addDomHandler(handler, MouseOutEvent.getType());
    }
}
