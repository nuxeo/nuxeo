/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: FakeEventListener.java 30799 2008-03-01 12:36:18Z bstefanescu $
 */

package org.nuxeo.ecm.core.repository;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.listener.AbstractEventListener;
import org.nuxeo.ecm.core.listener.AsynchronousEventListener;

/**
 * Fake event listener for tests.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class FakeEventListener extends AbstractEventListener implements
        AsynchronousEventListener {

    @SuppressWarnings("unchecked")
    public void handleEvent(CoreEvent coreEvent) {
        // add an info in the core event map when processing the event
        Map<String, ?> info = coreEvent.getInfo();
        if (info != null) {
            if (info.get("hits") != null) {
                ((List<String>) info.get("hits")).add(getName());
            }
        }
    }
}
