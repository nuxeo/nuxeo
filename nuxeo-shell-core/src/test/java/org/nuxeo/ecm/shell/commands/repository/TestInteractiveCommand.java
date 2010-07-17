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
