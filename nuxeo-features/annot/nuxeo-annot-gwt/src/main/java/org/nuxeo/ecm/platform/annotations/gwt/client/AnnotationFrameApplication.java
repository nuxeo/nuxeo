/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        Document.get().getBody().setScrollTop(controller.getFrameScrollFromTop());
    }

    private static void setListeners() {
        DOM.sinkEvents((com.google.gwt.user.client.Element) Document.get().cast(), Event.ONMOUSEMOVE | Event.ONCLICK
                | Event.ONMOUSEDOWN | Event.ONMOUSEUP);
        DOM.setEventListener((com.google.gwt.user.client.Element) Document.get().cast(), annotatedEventListener);
    }

}
