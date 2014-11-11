package org.nuxeo.runtime.testsuite;
import org.junit.After;
import org.junit.extensions.cpsuite.ClasspathSuite.ClassnameFilters;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.tracelog.internal.TraceEnabler;


@RunWith(IsolatedClasspathSuite.class)
@ClassnameFilters("org\\.nuxeo\\..*\\.Test.*")
public class RunAllRuntimeTests {

    TraceEnabler traceEnabler = new TraceEnabler();

    public void enableTrace() {
        traceEnabler.enable(true);
    }

    @After
    public void disableTrace() {
        traceEnabler.disable();
    }
}
