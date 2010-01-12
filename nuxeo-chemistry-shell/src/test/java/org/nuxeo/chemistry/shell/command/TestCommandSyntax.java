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

package org.nuxeo.chemistry.shell.command;

import org.junit.Assert;
import org.junit.Test;

public class TestCommandSyntax extends Assert {

    @Test
    public void testNoArgument() {
        CommandSyntax cs = CommandSyntax.parse("cmd");
        assertEquals(0, cs.getArguments().size());
        assertEquals("cmd", cs.getCommandToken().getName());
    }

    @Test
    public void testOneMandatoryArgument() {
        CommandSyntax cs = CommandSyntax.parse("cmd arg1");
        assertEquals(1, cs.getArguments().size());
        assertEquals("cmd", cs.getCommandToken().getName());
        assertEquals("arg1", cs.getArgument(0).getName());
    }

    @Test
    public void testOneOptionalArgument() {
        CommandSyntax cs = CommandSyntax.parse("cmd [arg1]");
        assertEquals(1, cs.getArguments().size());
        assertEquals("cmd", cs.getCommandToken().getName());
        assertEquals("arg1", cs.getArgument(0).getName());
    }

}
