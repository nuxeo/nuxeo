/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.event.test;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;

public class DummyPostCommitEventListener implements PostCommitEventListener {

    public static volatile int handledCount;

    @Override
    public void handleEvent(EventBundle events) throws ClientException {
        handledCount += 1;
    }

}
