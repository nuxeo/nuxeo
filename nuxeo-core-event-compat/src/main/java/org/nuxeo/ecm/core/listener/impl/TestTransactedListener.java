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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.listener.impl;

import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.operation.Operation;
import org.nuxeo.ecm.core.listener.AbstractEventListener;
import org.nuxeo.ecm.core.listener.OnSaveListener;
import org.nuxeo.ecm.core.listener.PostCommitListener;
import org.nuxeo.ecm.core.listener.PreCommitListener;

/**
 * This is a test listener that prints out transacted notifications.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TestTransactedListener extends AbstractEventListener implements PostCommitListener,
        OnSaveListener, PreCommitListener {

    public void onCommit(CoreEvent[] events) {
        System.out.println(">>>>> CORE_EVENTS > ON COMMIT: "+events.length);
    }

    public void onCommit(Operation<?>[] events) {
        System.out.println(">>>>> OPERATION_EVENTS > ON COMMIT: "+events.length);
    }

    public void onSave(CoreEvent[] events) {
        System.out.println(">>>>> CORE_EVENTS > ON SAVE: "+events.length);
    }

    public void onSave(Operation<?>[] events) {
        System.out.println(">>>>> OPERATION_EVENTS > ON SAVE: "+events.length);
    }

    public void aboutToCommit(CoreEvent[] events) {
        System.out.println(">>>>> CORE_EVENTS > ABOUT TO COMMIT: "+events.length);
    }

    public void aboutToCommit(Operation<?>[] events) {
        System.out.println(">>>>> OPERATION_EVENTS > ABOUT TO COMMIT: "+events.length);
    }

}
