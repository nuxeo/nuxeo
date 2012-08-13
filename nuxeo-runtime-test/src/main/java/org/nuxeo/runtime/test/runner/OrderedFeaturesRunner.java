package org.nuxeo.runtime.test.runner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

public class OrderedFeaturesRunner extends FeaturesRunner {

	public OrderedFeaturesRunner(Class<?> classToRun)
			throws InitializationError {
		super(classToRun);
	}
	
    @Override
    protected List<FrameworkMethod> computeTestMethods() {
    	List<FrameworkMethod> list = super.computeTestMethods();
    	List<FrameworkMethod> copy = new ArrayList<FrameworkMethod>(list);
    	Collections.sort(copy, new Comparator<FrameworkMethod>() {

			@Override
			public int compare(FrameworkMethod o1,
					FrameworkMethod o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
    	return copy;
    }

}
