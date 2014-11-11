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
 *     matic
 */
package org.nuxeo.ecm.platform.management.core.probes;

import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.management.probes.impl.RepositoryProbe;
import org.nuxeo.ecm.platform.management.probes.impl.RepositoryTestProbe;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public class TestRepositoryProbe extends RepositoryOSGITestCase {

   public void testRunRepositoryProbe() throws Exception {
       openRepositoryWithSystemPrivileges();
       RepositoryProbe probe = new RepositoryProbe();
       probe.runProbe(getCoreSession());
   }
   
   public void testRunRepositoryTestProbe() throws Exception {
       openRepositoryWithSystemPrivileges();
       RepositoryTestProbe probe = new RepositoryTestProbe();
       probe.runProbe(getCoreSession());
   }

}
