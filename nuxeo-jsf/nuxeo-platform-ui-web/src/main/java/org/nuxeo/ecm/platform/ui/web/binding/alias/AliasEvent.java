/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.binding.alias;

import javax.faces.component.UIComponent;
import javax.faces.event.FacesEvent;
import javax.faces.event.FacesListener;

import org.nuxeo.ecm.platform.ui.web.component.holder.UIValueHolder;

/**
 * Wrapper event used by {@link UIAliasHolder} and {@link UIValueHolder} components.
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
