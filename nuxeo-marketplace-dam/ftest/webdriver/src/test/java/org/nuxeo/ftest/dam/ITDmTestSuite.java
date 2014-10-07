/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Julien Carsique
 *
 */

package org.nuxeo.ftest.dam;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import org.nuxeo.ftest.dm.AbstractDmITSuite;

/**
 * Tests from DM package
 *
 * @since 5.9.6
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ AbstractDmITSuite.class })
public class ITDmTestSuite {

}
