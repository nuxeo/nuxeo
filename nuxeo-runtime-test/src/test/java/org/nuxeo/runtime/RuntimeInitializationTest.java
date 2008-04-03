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
 *
 * $Id$
 */

package org.nuxeo.runtime;

import junit.framework.AssertionFailedError;

import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RuntimeInitializationTest extends NXRuntimeTestCase {

    public void testContributions() {
        deployContrib("MyComp1.xml");
        deployContrib("MyComp2.xml");
    }

    // Deactivated for now since duplicate contributions are still allowed.
    public void XXXtestContributionsWithDuplicateComponent() {
        deployContrib("MyComp1.xml");
        deployContrib("MyComp2.xml");
        boolean success = false;
        try {
            deployContrib("CopyOfMyComp2.xml");
            success = true;
        } catch (AssertionFailedError e) {
            // OK.
        }
        assertFalse("An exception should have been raised.", success);
    }
}
