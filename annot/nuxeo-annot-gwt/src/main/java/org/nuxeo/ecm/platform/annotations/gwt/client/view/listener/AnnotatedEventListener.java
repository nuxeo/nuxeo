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
