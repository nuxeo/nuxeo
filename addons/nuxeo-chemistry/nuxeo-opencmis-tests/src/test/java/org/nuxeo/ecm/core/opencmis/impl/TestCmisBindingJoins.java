/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl;

import static org.junit.Assume.assumeTrue;

import org.junit.Before;

/**
 * Test the high-level session using a local connection.
 * <p>
 * Uses the QueryMaker that does CMISQL -> SQL, which allows JOINs.
 */
public class TestCmisBindingJoins extends TestCmisBinding {

    @Before
    public void checkNotDBS() throws Exception {
        assumeTrue("DBS does not support JOINs", !coreFeature.getStorageConfiguration().isDBS());
    }

    @Override
    protected boolean supportsJoins() {
        return true;
    }

}
