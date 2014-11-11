/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.shell.commands.repository;

import org.nuxeo.ecm.shell.commands.InteractiveCommand;

import edu.emory.mathcs.backport.java.util.Arrays;

import junit.framework.TestCase;

public class TestInteractiveCommand extends TestCase {

    public void testSplitArgs() {
        String[] args = InteractiveCommand.splitArgs("abc def ghj ikl mno pqr");
        String[] expected1 =  {"abc", "def", "ghj", "ikl", "mno", "pqr"};
        assertTrue(Arrays.deepEquals(expected1, args));
        args = InteractiveCommand.splitArgs("abc \"def ghj ikl\" mno pqr");
        String[] expected2 =  {"abc", "def ghj ikl", "mno", "pqr"};
        assertTrue(Arrays.deepEquals(expected2, args));
        args = InteractiveCommand.splitArgs("\"abc def ghj\" \"ikl mno pqr\"");
        String[] expected3 =  {"abc def ghj", "ikl mno pqr"};
        assertTrue(Arrays.deepEquals(expected3, args));
    }

}
