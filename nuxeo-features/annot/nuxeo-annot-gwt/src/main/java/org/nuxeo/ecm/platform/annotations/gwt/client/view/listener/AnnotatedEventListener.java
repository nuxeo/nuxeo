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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.view.listener;

import org.nuxeo.ecm.platform.annotations.gwt.client.AnnotationConstant;
import org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.CSSClassManager;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.annotater.Annotater;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.annotater.ImageAnnotater;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.annotater.TextAnnotater;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;

public class AnnotatedEventListener implements EventListener {
    private final Annotater imageAnnotater;

    private final Annotater textAnnotater;

    private final AnnotationController controller;

    private Annotater lastUsedAnnotater;

    public AnnotatedEventListener(AnnotationController controller) {
        this.imageAnnotater = new ImageAnnotater(controller);
        this.textAnnotater = new TextAnnotater(controller);
        this.controller = controller;
    }

    public void onBrowserEvent(Event event) {
        if (!controller.canAnnotate()) {
            return;
        }

        if (event.getTarget() == null) {// FF2 send a onload event.
            return;
        }

        if (event.getTypeInt() == Event.ONMOUSEOUT) {
            manageOnMouseOutEvent(event);
            return;
        }

        if (isOnImage(event)) {
            annotateImage(event);
        } else if (!controller.isImageOnly()) {
            annotateText(event);
        }
    }

    private void manageOnMouseOutEvent(Event event) {
        if (lastUsedAnnotater != null) {
            lastUsedAnnotater.manageEvent(event);
            lastUsedAnnotater = null;
        }
    }

    private boolean isOnImage(Event event) {
        Element element = event.getTarget();
        if (element.getNodeName().equalsIgnoreCase("div")) {
            CSSClassManager cssManager = new CSSClassManager(element);
            if (cssManager.isClassPresent(AnnotationConstant.DECORATE_CLASS_NAME)
                    || cssManager.isClassPresent(AnnotationConstant.DECORATE_NOT_CLASS_NAME)
                    || cssManager.isClassPresent(AnnotationConstant.DECORATE_AREA_CLASS_NAME)) {
                return true;
            }
        } else if (element.getNodeName().equalsIgnoreCase("img")) {
            return true;
        }
        return false;
    }

    private void annotateImage(Event event) {
        imageAnnotater.manageEvent(event);
    }

    private void annotateText(Event event) {
        textAnnotater.manageEvent(event);
        lastUsedAnnotater = textAnnotater;
    }

}
