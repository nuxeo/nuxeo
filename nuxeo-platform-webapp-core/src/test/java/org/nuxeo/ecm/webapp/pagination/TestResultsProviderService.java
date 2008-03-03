/*
 * (C) Copyright 2002 - 2006 Nuxeo SARL <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.pagination;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="gracinet@nuxeo.com">Georges Racinet</a>
 */
public class TestResultsProviderService extends NXRuntimeTestCase {

    private ResultsProviderService service;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deploy("test-resultsprovider-service.xml");
        deploy("resultsprovider-components-test-setup.xml");

        service = (ResultsProviderService) Framework.getRuntime()
                        .getComponent(ResultsProviderService.NAME);
    }

    public void testRegistration() throws Exception {
        assertNotNull(service);
        assertEquals("searchActions", service.getFarmNameFor("MY_SEARCH"));
        assertEquals("dashboardsActions",
                     service.getFarmNameFor("MY_DOCUMENTS"));
    }

}
