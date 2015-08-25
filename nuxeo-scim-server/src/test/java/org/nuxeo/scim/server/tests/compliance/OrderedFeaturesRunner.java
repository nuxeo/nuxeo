package org.nuxeo.scim.server.tests.compliance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

public class OrderedFeaturesRunner extends FeaturesRunner {

    public OrderedFeaturesRunner(Class<?> arg0) throws InitializationError {
        super(arg0);
    }
    
    @Override
    protected List computeTestMethods() {
        List list = super.computeTestMethods();
        List copy = new ArrayList(list);
        Collections.sort(copy, new Comparator<FrameworkMethod>() {
            public int compare(FrameworkMethod o1, FrameworkMethod o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return copy;
    }
}
