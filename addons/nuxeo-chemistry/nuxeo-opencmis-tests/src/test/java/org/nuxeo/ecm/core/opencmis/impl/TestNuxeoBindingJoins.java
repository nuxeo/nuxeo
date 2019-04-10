/*
 * Copyright (c) 2014 Nuxeo SA (http://nuxeo.com/) and others.
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

/**
 * Tests that hit directly the server APIs.
 * <p>
 * Uses the QueryMaker that does CMISQL -> SQL, which allows JOINs.
 */
public class TestNuxeoBindingJoins extends TestNuxeoBinding {

    @Override
    protected boolean supportsJoins() {
        return true;
    }

}
