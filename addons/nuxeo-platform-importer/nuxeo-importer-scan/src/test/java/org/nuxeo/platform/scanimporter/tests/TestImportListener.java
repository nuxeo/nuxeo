/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Thierry Delprat
 */
package org.nuxeo.platform.scanimporter.tests;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.platform.scanimporter.listener.IngestionTrigger;

public class TestImportListener extends ImportTestCase {

    @Test
    public void testTrigger() throws Exception {

        EventContext ctx= new EventContextImpl(null,null);
        ctx.setProperty("Testing", true);
        Event evt = ctx.newEvent(IngestionTrigger.START_EVENT);

        IngestionTrigger listener = new IngestionTrigger();
        listener.handleEvent(evt);

        assertNotNull(evt.getContext().getProperty("Tested"));
    }
}
