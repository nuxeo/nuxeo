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

package org.nuxeo.ecm.platform.annotations.gwt.client.view.annotater;

import org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.AnnotationChangeListener;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.AnnotationModel;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.Utils;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.XPathUtil;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.NewAnnotationPopup;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.decorator.ImageDecorator;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;

/**
 * @author Alexandre Russel
 */
public class ImageAnnotater extends AbstractAnnotater implements
        AnnotationChangeListener {
    private boolean writing = false;

    private boolean processing = false;

    private final ImageDecorator decorator;

    private DivElement divElement;

    private int ax = -1;

    private int ay = -1;

    private int bx = -1;

    private int by = -1;

    private ImageElement image;

    public ImageAnnotater(AnnotationController controller) {
        super(controller, true);
        decorator = new ImageDecorator(controller);
        controller.addModelChangeListener(this);
    }

    public void refresh() {
        ax = -1;

        ay = -1;

        bx = -1;

        by = -1;
    }

    @Override
    public void onMouseDown(Event event) {
        super.onMouseDown(event);
        if (writing /* || processing */) {
            Log.debug("ImageAnnotater] Ignore mouse down event");
            return;
        }
        if (processing) {
            divElement.getParentElement().removeChild(divElement);
        }
        image = getRootImage(event);
        int[] absoluteTopLeft = Utils.getAbsoluteTopLeft(image, Document.get());
        ax = event.getClientX() - absoluteTopLeft[1]
                + image.getOwnerDocument().getBody().getScrollLeft();
        ay = event.getClientY() - absoluteTopLeft[0]
                + image.getOwnerDocument().getBody().getScrollTop();
        bx = ax;
        by = ay;
        writing = true;
        processing = true;
        controller.disablePopupListeners();
        addMap(ax, ay, bx, by, image);
    }

    private ImageElement getRootImage(Event event) {
        com.google.gwt.dom.client.Element targetElement = event.getTarget();
        ImageElement imageElement = ImageElement.as(targetElement.getOwnerDocument().getElementById(
                "annotationRootImage"));
        if (imageElement == null) {
            if (targetElement.getNodeName().equalsIgnoreCase("img")) {
                imageElement = ImageElement.as(targetElement);
            } else if (targetElement.getNodeName().equalsIgnoreCase("div")) {
                imageElement = getImageElementFromAnchor(targetElement);
            }
        }
        return imageElement;
    }

    private static ImageElement getImageElementFromAnchor(
            com.google.gwt.dom.client.Element anchorElement) {
        Node element;
        while ((element = anchorElement.getPreviousSibling()) != null) {
            Log.debug("getImageElementFromAnchor -- nodeName: "
                    + element.getNodeName());
            if (element.getNodeName().equalsIgnoreCase("img")) {
                return ImageElement.as((Element) element.cast());
            }
        }
        return null;
    }

    @Override
    public void onMouseMove(Event event) {
        super.onMouseMove(event);

        if (!writing) {
            return;
        }
        String nodeName = event.getTarget().getNodeName();
        if (nodeName.equalsIgnoreCase("img")) {
            ImageElement newImage = ImageElement.as(event.getTarget());
            if ((!image.equals(newImage) || ax == -1 || ay == -1)
                    && !controller.isMultiImage()) {
                refresh();
            }
        }
        int[] absoluteTopLeft = Utils.getAbsoluteTopLeft(image, Document.get());
        bx = event.getClientX() - absoluteTopLeft[1]
                + image.getOwnerDocument().getBody().getScrollLeft();
        by = event.getClientY() - absoluteTopLeft[0]
                + image.getOwnerDocument().getBody().getScrollTop();
        updateMap(ax, ay, bx, by, image);
    }

    @Override
    public void onMouseUp(Event event) {
        if (!hasMoved() && writing) {
            Log.debug("cancel mouse up image");
            cancelMap();
            controller.setNewAnnotationPopup(null);
            if (controller.isAnnotationsVisible()) {
                controller.enablePopupListeners();
            }
            super.onMouseUp(event);
            return;
        }

        super.onMouseUp(event);

        if (!writing) {
            return;
        }
        String nodeName = event.getTarget().getNodeName();
        if (nodeName.equalsIgnoreCase("img")) {
            ImageElement newImage = ImageElement.as(event.getTarget());
            if ((!image.equals(newImage) || ax == -1 || ay == -1)
                    && !controller.isMultiImage()) {
                refresh();
            }
        }

        int[] absoluteTopLeft = Utils.getAbsoluteTopLeft(image, Document.get());
        bx = event.getClientX() - absoluteTopLeft[1]
                + image.getOwnerDocument().getBody().getScrollLeft();
        by = event.getClientY() - absoluteTopLeft[0]
                + image.getOwnerDocument().getBody().getScrollTop();
        addMapAndGetAnnot(new int[] { ax, ay, bx, by }, image);
        if (controller.isAnnotationsVisible()) {
            controller.enablePopupListeners();
        }
        writing = false;
        addAnnotationPopup();
        controller.enablePopupListeners();
    }

    private void cancelMap() {
        writing = false;
        processing = false;
        if (divElement != null) {
            DOM.setEventListener(
                    (com.google.gwt.user.client.Element) divElement.cast(), null);
            Log.debug("Parent element: " + divElement.getParentElement());
            if (divElement.getParentElement() != null) {
                divElement.getParentElement().removeChild(divElement);
            }
        }
    }

    public void updateMap(int ax2, int ay2, int bx2, int by2, ImageElement img) {
        decorator.updateAnnotatedArea(ax2, ay2, bx2, by2, img, divElement);
    }

    private void addMap(int ax2, int ay2, int ax3, int ay3, ImageElement img) {
        divElement = decorator.addAnnotatedArea(ax, ay, bx, by, this);
    }

    public void addMapAndGetAnnot(int[] points, ImageElement img) {
        DOM.setEventListener(
                (com.google.gwt.user.client.Element) divElement.cast(), null);
        String xpath = img.getParentElement().getId();
        xpath = XPathUtil.fromIdableName(xpath);
        checkInt(points);
        String xpointer = controller.filterXPointer(image, xpath, points[0],
                points[1], points[2], points[3]);
        Log.debug("XPointer: " + xpointer);
        controller.createNewAnnotation(xpointer);
        NewAnnotationPopup popup = new NewAnnotationPopup(divElement, controller,
                true, "local");
        controller.setNewAnnotationPopup(popup);
    }

    private static void checkInt(int[] points) {
        // following code is because, on some IE machine we got float instead of
        // integer:
        for (int x = 0; x < points.length; x++) {
            points[x] = ("" + points[x]).contains(".") ? Integer.parseInt(("" + points[x]).substring(
                    0, ("" + points[x]).indexOf(".")))
                    : points[x];
        }
    }

    public ImageElement getImage() {
        return image;
    }

    public void setImage(ImageElement image) {
        this.image = image;
    }

    public void updateMap(int bx2, int by2, ImageElement image2) {
        decorator.updateAnnotatedArea(ax, ay, bx2, by2, image2, divElement);
    }

    public void onChange(AnnotationModel model, ChangeEvent ce) {
        if (model.getNewAnnotation() == null && ce == ChangeEvent.annotation) {
            processing = false;
        }
    }

}
