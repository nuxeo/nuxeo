/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: EditableModelRowEvent.java 28493 2008-01-04 19:51:30Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.model.impl;

import javax.faces.component.UIComponent;
import javax.faces.event.FacesEvent;
import javax.faces.event.FacesListener;
import javax.faces.event.PhaseId;

/**
 * EditableModel row event
 * <p>
 * Row event wraps the original event to put it in the row context.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class EditableModelRowEvent extends FacesEvent {

    private static final long serialVersionUID = -2537709468370440334L;

    private final FacesEvent event;

    private final Integer key;

    public EditableModelRowEvent(UIComponent source, FacesEvent wrapped, Integer key) {
        super(source);
        event = wrapped;
        this.key = key;
    }

    @Override
    public PhaseId getPhaseId() {
        return event.getPhaseId();
    }

    @Override
    public void setPhaseId(PhaseId phaseId) {
        event.setPhaseId(phaseId);
    }

    @Override
    public void processListener(FacesListener listener) {
        // This event is never delivered to a listener
        throw new IllegalStateException();
    }

    @Override
    public boolean isAppropriateListener(FacesListener listener) {
        // This event is never delivered to a listener
        return false;
    }

    public FacesEvent getEvent() {
        return event;
    }

    public Integer getKey() {
        return key;
    }

}
