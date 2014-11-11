/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.themes;

import org.nuxeo.theme.Manager;
import org.nuxeo.theme.events.AbstractEventListener;
import org.nuxeo.theme.events.EventContext;
import org.nuxeo.theme.events.EventType;

public class StylesModifiedListener extends AbstractEventListener {

    public StylesModifiedListener() {
    }

    public StylesModifiedListener(EventType eventType) {
        super(eventType);
    }

    @Override
    public void handle(EventContext context) {
        Manager.getThemeManager().stylesModified();
    }

}
