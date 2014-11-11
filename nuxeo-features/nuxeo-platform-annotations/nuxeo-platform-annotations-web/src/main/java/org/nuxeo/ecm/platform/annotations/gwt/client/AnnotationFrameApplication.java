/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.platform.annotations.gwt.client;

import org.nuxeo.ecm.platform.annotations.gwt.client.configuration.WebConfiguration;
import org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.listener.AnnotatedEventListener;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public class AnnotationFrameApplication {

    private static WebConfiguration WEB_CONFIGURATION;

    private static AnnotationController controller;

    private static AnnotatedEventListener annotatedEventListener;

    public static AnnotationController getController() {
        return controller;
    }

    public static AnnotatedEventListener getMainEventListener() {
        return annotatedEventListener;
    }

    public static void build(WebConfiguration webConfiguration) {
        WEB_CONFIGURATION = webConfiguration;
        buildApplication();
    }

    private static void buildApplication() {
        controller = new AnnotationController(WEB_CONFIGURATION, true);
        annotatedEventListener = new AnnotatedEventListener(controller);
        configureController();
        setListeners();
        controller.loadAnnotations();
        notifyFrameModuleInitialized();
    }

    private static native void notifyFrameModuleInitialized() /*-{
       top['frameModuleInitialized'] = true;
    }-*/;

    private static void configureController() {
        PreviewSettings previewSettings = PreviewSettings.getInstance();
        Log.debug("previewSettings = " + previewSettings);
        if (previewSettings != null) {
            controller.setImageOnly(previewSettings.isImageOnly());
            controller.setMultiImage(previewSettings.isMultiImageAnnotation());
            controller.setXPointerFilter(previewSettings.getXPointerFilterPath());
            controller.setPointerAdapter(previewSettings.getPointerAdapter());
            controller.setAnnotationDecoratorFunction(previewSettings.getAnnotationDecoratorFunction());
        }
        Document.get().getBody().setScrollTop(
                controller.getFrameScrollFromTop());
    }

    private static void setListeners() {
        DOM.sinkEvents(
                (com.google.gwt.user.client.Element) Document.get().cast(),
                Event.ONMOUSEMOVE | Event.ONCLICK | Event.ONMOUSEDOWN
                        | Event.ONMOUSEUP);
        DOM.setEventListener(
                (com.google.gwt.user.client.Element) Document.get().cast(),
                annotatedEventListener);
    }

}
