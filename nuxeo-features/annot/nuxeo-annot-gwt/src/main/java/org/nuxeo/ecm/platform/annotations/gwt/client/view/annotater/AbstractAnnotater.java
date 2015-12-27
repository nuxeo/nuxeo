/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.view.annotater;

import org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public abstract class AbstractAnnotater implements Annotater {

    protected final AnnotationController controller;

    private final boolean eventPreventDefault;

    private boolean onMouseDown, onMouseMove = false;

    public AbstractAnnotater(AnnotationController controller, boolean eventPreventDefault) {
        this.controller = controller;
        this.eventPreventDefault = eventPreventDefault;
    }

    public void manageEvent(Event event) {
        if (!controller.canCreateNewCreationPopup() || !controller.canAnnotate()) {
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
