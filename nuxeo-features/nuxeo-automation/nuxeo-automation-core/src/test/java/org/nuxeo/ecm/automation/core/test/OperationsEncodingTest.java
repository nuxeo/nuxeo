/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.core.events.EventHandler;
import org.nuxeo.ecm.automation.core.events.EventHandlerRegistry;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.automation.core" })
@LocalDeploy("org.nuxeo.ecm.automation.core:test-enc.xml")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class OperationsEncodingTest {

    @Inject
    EventHandlerRegistry reg;

    @Test
    public void testXmlEncoding() {
        List<EventHandler> eh = reg.getEventHandlers("aboutToCreate");
        assertEquals("a < b & b > c", eh.get(0).getCondition());
    }

}
