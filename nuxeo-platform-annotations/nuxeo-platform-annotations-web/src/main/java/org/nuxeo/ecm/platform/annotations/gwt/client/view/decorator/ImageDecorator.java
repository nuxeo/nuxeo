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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.view.decorator;

import org.nuxeo.ecm.platform.annotations.gwt.client.AnnotationConstant;
import org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.Annotation;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.annotater.ImageAnnotater;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.listener.AnnotationPopupEventListener;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
 */
public class ImageDecorator {

    private AnnotationController controller;

    public ImageDecorator(AnnotationController controller) {
        this.controller = controller;
    }

    public DivElement addAnnotatedArea(final int ax, final int ay,
            final int bx, final int by, final ImageAnnotater img) {
        int top = (by > ay ? ay : by);
        int height = Math.abs(ay - by);
        int left = (ax > bx ? bx : ax);
        int width = Math.abs(ax - bx);
        final DivElement divElement = img.getImage().getOwnerDocument().createDivElement();
        divElement.setClassName(AnnotationConstant.IGNORED_ELEMENT + " "
                + AnnotationConstant.DECORATE_AREA_CLASS_NAME);
        divElement.getStyle().setProperty("left", "" + left + "px");
        divElement.getStyle().setProperty("top", "" + top + "px");
        divElement.getStyle().setProperty("width", "" + width + "px");
        divElement.getStyle().setProperty("height", "" + height + "px");
        divElement.getStyle().setProperty("display", "block");
        DOM.sinkEvents((Element) divElement.cast(), Event.ONMOUSEMOVE
                | Event.ONMOUSEUP);
        DOM.setEventListener((Element) divElement.cast(),
                new EventListener() {
                    public void onBrowserEvent(Event event) {
                        img.manageEvent(event);
                    }
                });
        img.getImage().getParentElement().appendChild(divElement);
        return divElement;
    }

    public void updateAnnotatedArea(int ax, int ay, int bx, int by,
            ImageElement img, DivElement divElement) {
        int top = (by > ay ? ay : by) + img.getOffsetTop();
        int height = Math.abs(ay - by);
        int left = (ax > bx ? bx : ax) + img.getOffsetLeft();
        int width = Math.abs(ax - bx);
        divElement.getStyle().setProperty("left", "" + left + "px");
        divElement.getStyle().setProperty("top", "" + top + "px");
        divElement.getStyle().setProperty("width", "" + width + "px");
        divElement.getStyle().setProperty("height", "" + height + "px");
    }

    public void addAnnotatedArea(int ax, int ay, int bx, int by,
            ImageElement img, Annotation annotation,
            AnnotationController controller) {
        int top = (by > ay ? ay : by) + img.getOffsetTop();
        int height = Math.abs(ay - by);
        int left = (ax > bx ? bx : ax) + img.getOffsetLeft();
        int width = Math.abs(ax - bx);
        Element element = null;
        element = createDivElement(img, annotation, top, height, left, width);
        DOM.sinkEvents(element, Event.ONMOUSEOVER | Event.ONMOUSEOUT);
        DOM.setEventListener(element,
                AnnotationPopupEventListener.getAnnotationPopupEventListener(
                        annotation, controller));
    }

    private Element createDivElement(ImageElement img, Annotation annotation, int top,
            int height, int left, int width) {
        DivElement divElement = img.getOwnerDocument().createDivElement();
        divElement.setClassName(controller.getDecorateClassName() + " "
                + AnnotationConstant.DECORATE_CLASS_NAME + annotation.getId());
        divElement.getStyle().setProperty("left", "" + left + "px");
        divElement.getStyle().setProperty("top", "" + top + "px");
        divElement.getStyle().setProperty("width", "" + width + "px");
        divElement.getStyle().setProperty("height", "" + height + "px");
        divElement.getStyle().setProperty("display", "block");
        divElement.getStyle().setProperty("position", "absolute");
        img.getParentElement().appendChild(divElement);
        return (Element) divElement.cast();
    }
}
