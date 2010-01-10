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
