package org.nuxeo.runtime.deployment.preprocessor;

import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

public class DeploymentActivator implements BundleActivator, BundleTrackerCustomizer {

    protected static DeploymentActivator me;

	protected DeploymentPreprocessor preprocessor;

	@Override
	public void start(BundleContext context) throws Exception {
        String v  = Framework.getProperty("org.nuxeo.app.preprocessing");
        if (v != null) {
            if(!Boolean.parseBoolean(v)) {
            	return;
            }
        }
        preprocessor = new DeploymentPreprocessor(Framework.getRuntime().getHome());
        preprocessor.init();
		new BundleTracker(context, Bundle.ACTIVE, this).open();
		me = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		preprocessor = null;
		me = null;
	}

	@Override
	public Object addingBundle(Bundle bundle, BundleEvent event) {
		try {
            preprocessor.processBundle(bundle);
        } catch (Exception e) {
            LogFactory.getLog(DeploymentActivator.class).error("Cannot preprocess fragment from " + bundle, e);
        }
		return bundle;
	}

	@Override
	public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
		;
	}

	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
		preprocessor.forgetBundle(bundle);
	}


}
