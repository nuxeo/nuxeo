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

package org.nuxeo.ecm.platform.annotations.gwt.client.view.annotater;

import org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public abstract class AbstractAnnotater implements Annotater {

    protected final AnnotationController controller;

    private final boolean eventPreventDefault;

    private boolean onMouseDown, onMouseMove = false;

    public AbstractAnnotater(AnnotationController controller,
            boolean eventPreventDefault) {
        this.controller = controller;
        this.eventPreventDefault = eventPreventDefault;
    }

    public void manageEvent(Event event) {
        if (!controller.canCreateNewCreationPopup()
                || !controller.canAnnotate()) {
            return;
        }

        if (eventPreventDefault) {
            DOM.eventPreventDefault(event);
        }

        switch (event.getTypeInt()) {
        case Event.ONMOUSEDOWN:
            onMouseDown(event);
            break;
        case Event.ONMOUSEMOVE:
            onMouseMove(event);
            break;
        case Event.ONMOUSEUP:
            onMouseUp(event);
            break;
        case Event.ONMOUSEOUT:
            onMouseOut(event);
            break;
        }
    }

    public void onMouseDown(Event event) {
        onMouseDown = true;
    }

    public void onMouseMove(Event event) {
        if (onMouseDown) {
            onMouseMove = true;
        }
    }

    public void onMouseUp(Event event) {
        onMouseDown = onMouseMove = false;
    }

    public void onMouseOut(Event event) {
        onMouseUp(event);
    }

    public boolean hasMoved() {
        return onMouseMove;
    }

    protected void addAnnotationPopup() {
        controller.addNewAnnotation();
    }

}
