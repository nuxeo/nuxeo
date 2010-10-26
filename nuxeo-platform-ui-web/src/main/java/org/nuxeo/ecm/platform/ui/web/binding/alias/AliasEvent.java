/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.binding.alias;

import javax.faces.component.UIComponent;
import javax.faces.event.FacesEvent;
import javax.faces.event.FacesListener;

/**
 * Wrapper event used by {@link UIAliasHolder} component
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class AliasEvent extends FacesEvent {

    private static final long serialVersionUID = 1L;

    protected final FacesEvent originalEvent;

    public AliasEvent(UIComponent component, FacesEvent originalEvent) {
        super(component);
        this.originalEvent = originalEvent;
        setPhaseId(originalEvent.getPhaseId());
    }

    public FacesEvent getOriginalEvent() {
        return originalEvent;
    }

    @Override
    public boolean isAppropriateListener(FacesListener listener) {
        return originalEvent.isAppropriateListener(listener);
    }

    @Override
    public void processListener(FacesListener listener) {
        originalEvent.processListener(listener);
    }

}
