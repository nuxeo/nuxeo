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

package org.nuxeo.theme.test.configuration;

import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.negotiation.NegotiationType;
import org.nuxeo.theme.test.Scheme1;
import org.nuxeo.theme.test.Scheme2;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;

public class TestNegotiationConfiguration extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-contrib.xml");
        deployContrib("org.nuxeo.theme.core.tests", "negotiation-config.xml");
    }

    public void testRegisterNegotiation() {
        TypeRegistry typeRegistry = Manager.getTypeRegistry();
        // negotiation
        NegotiationType negotiation1 = (NegotiationType) typeRegistry.lookup(
                TypeFamily.NEGOTIATION, "strategy1/test negotiation");
        assertNotNull(negotiation1);
        assertEquals("strategy1", negotiation1.getStrategy());

        assertSame(Scheme1.class, negotiation1.getSchemes().get(0).getClass());
        assertSame(Scheme2.class, negotiation1.getSchemes().get(1).getClass());
    }

}
