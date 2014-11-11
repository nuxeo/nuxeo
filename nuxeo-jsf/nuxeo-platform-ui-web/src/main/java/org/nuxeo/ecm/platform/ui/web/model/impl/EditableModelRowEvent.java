/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
