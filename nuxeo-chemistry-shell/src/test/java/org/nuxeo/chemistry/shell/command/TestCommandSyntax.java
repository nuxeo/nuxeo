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
        assertEquals(1, cs.getTokens().size());
        assertEquals("cmd", cs.getCommandToken().getName());
    }

    @Test
    public void testOneMandatoryArgument() {
        CommandSyntax cs = CommandSyntax.parse("cmd arg1");
        assertEquals(1, cs.getArguments().size());
        assertEquals(2, cs.getTokens().size());

        CommandToken cmd = cs.getCommandToken();
        assertEquals("cmd", cmd.getName());
        assertTrue(cmd.isCommand());
        assertFalse(cmd.isArgument());

        CommandToken arg1 = cs.getArgument(0);
        CommandToken arg1ByName = cs.getToken("arg1");
        assertEquals(arg1ByName, arg1);

        assertEquals("arg1", arg1.getName());
        assertTrue(arg1.isArgument());
        assertFalse(arg1.isCommand());
        assertFalse(arg1.isOptional());
        assertFalse(arg1.isFlag());
        assertNull(arg1.getValueType());
    }

    @Test
    public void testOneMandatoryWithType() {
        CommandSyntax cs = CommandSyntax.parse("cmd arg1:file");
        assertEquals(1, cs.getArguments().size());
        assertEquals(2, cs.getTokens().size());

        CommandToken arg1 = cs.getArgument(0);
        CommandToken arg1ByName = cs.getToken("arg1");
        assertEquals(arg1ByName, arg1);

        assertEquals("arg1", arg1.getName());
        assertTrue(arg1.isArgument());
        assertFalse(arg1.isCommand());
        assertFalse(arg1.isFlag());

        assertEquals("file", arg1.getValueType());
    }

    @Test
    public void testOneOptionalArgument() {
        CommandSyntax cs = CommandSyntax.parse("cmd [arg1]");
        assertEquals(1, cs.getArguments().size());
        assertEquals(2, cs.getTokens().size());

        CommandToken cmd = cs.getCommandToken();
        assertEquals("cmd", cmd.getName());
        assertTrue(cmd.isCommand());
        assertFalse(cmd.isArgument());

        CommandToken arg1 = cs.getArgument(0);
        CommandToken arg1ByName = cs.getToken("arg1");
        assertEquals(arg1ByName, arg1);

        assertEquals("arg1", arg1.getName());
        assertTrue(arg1.isArgument());
        assertFalse(arg1.isCommand());
        assertTrue(arg1.isOptional());
        assertFalse(arg1.isFlag());
    }

    @Test
    public void testOneOptionalFlag() {
        CommandSyntax cs = CommandSyntax.parse("cmd [-s]");
        assertEquals(0, cs.getArguments().size());
        assertEquals(2, cs.getTokens().size());

        CommandToken cmd = cs.getCommandToken();
        assertEquals("cmd", cmd.getName());
        assertTrue(cmd.isCommand());
        assertFalse(cmd.isArgument());

        CommandToken flag = cs.getToken("-s");

        assertEquals("-s", flag.getName());
        assertFalse(flag.isArgument());
        assertFalse(flag.isCommand());
        assertTrue(flag.isOptional());
        assertTrue(flag.isFlag());
    }

    @Test
    public void testDefaultValue() {
        CommandSyntax cs = CommandSyntax.parse("cmd [arg1:file?toto]");
        CommandToken arg1 = cs.getToken("arg1");
        assertEquals("arg1", arg1.getName());
        assertEquals("file", arg1.getValueType());
        assertEquals("toto", arg1.getDefaultValue());
        assertTrue(arg1.isArgument());
        assertFalse(arg1.isCommand());
        assertTrue(arg1.isOptional());
        assertFalse(arg1.isFlag());
    }
}
