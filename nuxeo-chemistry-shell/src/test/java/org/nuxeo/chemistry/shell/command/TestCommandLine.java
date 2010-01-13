package org.nuxeo.chemistry.shell.command;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.chemistry.shell.Application;

import java.util.HashMap;
import java.util.Map;

public class TestCommandLine extends Assert {

    CommandRegistry reg;

    @Before
    public void setUp() {
        Map<String,Command> map = new HashMap<String,Command>();
        map.put("cmd", new DummyCommand());
        reg = new CommandRegistry(map);
    }

    @Test
    public void testWithoutOption() throws CommandException {
        String line = "cmd tata";
        CommandLine commandLine = new CommandLine(reg, line);

        assertEquals("cmd", commandLine.getCommand().getName());
        assertEquals("tata", commandLine.getParameterValue("toto"));
        assertNull(commandLine.getParameter("-r"));
        //assertEquals("tutu", commandLine.getParameterValue("titi"));
    }

    @Test
    public void testWithOption() throws CommandException {
        String line = "cmd -r tata";
        CommandLine commandLine = new CommandLine(reg, line);

        assertEquals("cmd", commandLine.getCommand().getName());
        assertEquals("tata", commandLine.getParameterValue("toto"));
        assertNotNull(commandLine.getParameter("-r"));
        //assertEquals("tutu", commandLine.getParameterValue("titi"));
    }

    @Ignore // this is not a test, you dummy Maven!
    @Cmd(syntax="cmd [-r] toto [titi:file?tutu]", synopsis="")
    class DummyCommand extends AnnotatedCommand {
        public void run(Application app, CommandLine cmdLine) throws Exception {}
    }

}

