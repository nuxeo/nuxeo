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

import org.nuxeo.ecm.webengine.gwt.client.Framework;
import org.nuxeo.ecm.webengine.gwt.client.UI;
import org.nuxeo.ecm.webengine.gwt.client.http.HttpRequest;
import org.nuxeo.ecm.webengine.gwt.client.http.HttpResponse;

import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class View extends Composite {

    protected String name;

    protected View(String name) {
        this.name = name;
        initWidget(createContent());
    }

    public View(String name, Widget widget) {
        this.name = name;
        initWidget(widget);
    }

    /**
     * Must override to create the content widget
     * This method is not used if the item is not subclassed
     * @return
     */
    protected Widget createContent() {
        throw new IllegalStateException("This method must be overrided when Item class is subclassed");
    }

    public String getName() {
        return name;
    }


    /**
     * @return the icon.
     */
    public Image getIcon() {
        return UI.getEmptyImage();
    }


    public String getHeader() {
        return getHeaderString(getTitle(), getIcon());
    }

    /**
     * Called by the container (if container supports refresh)
     * when application context change
     */
    public void refresh() {

    }


    /**
     * Get a string representation of the header that includes an image and some
     * text.
     *
     * @param text the header text
     * @param image the {@link AbstractImagePrototype} to add next to the header
     * @return the header as a string
     */
    public static String getHeaderString(String text, Image image) {
        // Add the image and text to a horizontal panel
        HorizontalPanel hPanel = new HorizontalPanel();
        hPanel.setSpacing(0);
        hPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        hPanel.add(image);
        HTML headerText = new HTML(text);
        headerText.setStyleName("cw-StackPanelHeader");
        hPanel.add(headerText);

        // Return the HTML string for the panel
        return hPanel.getElement().getString();
    }


    public void showBusy() {
        UI.showBusy();
    }

    public void hideBusy() {
        UI.hideBusy();
    }

    public ViewRequest get(String uri) {
        return new ViewRequest(this, RequestBuilder.GET, uri);
    }

    public ViewRequest post(String uri) {
        return new ViewRequest(this, RequestBuilder.POST, uri);
    }

    public void onRequestSuccess(HttpRequest request, HttpResponse response) {
        hideBusy();
        onRequestCompleted(request, response);
    }

    /**
     * Override this when needed
     * @param response
     */
    public void onRequestCompleted(HttpRequest request, HttpResponse response) {
        // do nothing
    }

    public void onRequestFailure(HttpRequest request, Throwable cause) {
        hideBusy();
        Framework.handleError(cause);
    }

}
