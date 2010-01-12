package org.nuxeo.chemistry.shell;

import org.junit.Assert;
import org.junit.Test;

public class TestWithTestScript extends Assert {

    @Test
    public void test() throws Exception {
        Main.main(new String[] {"cmissh", "-t", "-b", "testscript"});
    }

}
