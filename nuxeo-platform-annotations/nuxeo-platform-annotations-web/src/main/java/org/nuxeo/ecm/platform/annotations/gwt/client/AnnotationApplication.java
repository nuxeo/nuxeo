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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client;

import org.nuxeo.ecm.platform.annotations.gwt.client.configuration.WebConfiguration;
import org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.AnnotationManagerPanel;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.HideManagerButton;

import com.google.gwt.dom.client.BaseElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public class AnnotationApplication {

    private static WebConfiguration WEB_CONFIGURATION;

    private static DockPanel applicationPanel = new DockPanel();

    private static Frame PREVIEW_FRAME;

    public static void build(WebConfiguration webConfiguration) {
        WEB_CONFIGURATION = webConfiguration;
        buildApplication();
    }

    private static void buildApplication() {
        AnnotationConfiguration annotationConfiguration = AnnotationConfiguration.getInstance();
        Document document = Document.get();
        BaseElement baseElement = document.getElementsByTagName("base").getItem(
                0).cast();
        registerBaseHref(baseElement.getHref());
        registerAnnoteaServerUrl(annotationConfiguration.getAnnoteaServerUrl());
        registerDocUrl(annotationConfiguration.getDocumentUrl());
        notifyAnnoteaServerUrlRegistered();

        applicationPanel.setStyleName("annotationApplicationPanel");
        applicationPanel.setWidth("100%");

        RootPanel display = RootPanel.get("display");
        String height = Integer.toString(display.getOffsetHeight()) + "px";
        applicationPanel.setHeight(height);
        applicationPanel.setHorizontalAlignment(DockPanel.ALIGN_LEFT);

        PREVIEW_FRAME = new Frame(annotationConfiguration.getPreviewUrl());
        PREVIEW_FRAME.setStyleName("previewFrame");
        PREVIEW_FRAME.setWidth("100%");
        PREVIEW_FRAME.setHeight(height);
        applicationPanel.add(PREVIEW_FRAME, DockPanel.CENTER);
        applicationPanel.setCellWidth(PREVIEW_FRAME, "100%");

        AnnotationController controller = new AnnotationController(
                WEB_CONFIGURATION, false);
        AnnotationManagerPanel annotationManagerPanel = new AnnotationManagerPanel(
                controller, WEB_CONFIGURATION);
        controller.addModelChangeListener(annotationManagerPanel);

        HideManagerButton hideManagerButton = new HideManagerButton(controller,
                annotationManagerPanel, PREVIEW_FRAME);

        annotationManagerPanel.setWidth("250px");
        applicationPanel.add(annotationManagerPanel, DockPanel.WEST);

        hideManagerButton.setHeight(height);
        applicationPanel.add(hideManagerButton, DockPanel.WEST);

        display.add(applicationPanel);

        controller.loadAnnotations();
    }

    private static native void registerBaseHref(String baseHref) /*-{
        top['baseHref'] = baseHref;
    }-*/;

    private static native void registerAnnoteaServerUrl(String url) /*-{
        top['annoteaServerUrl'] = url;
    }-*/;

    private static native void notifyAnnoteaServerUrlRegistered() /*-{
        top['annoteaServerUrlRegistered'] = true;
    }-*/;

    private static native void registerDocUrl(String docUrl) /*-{
        top['docUrl'] = docUrl;
    }-*/;

}
