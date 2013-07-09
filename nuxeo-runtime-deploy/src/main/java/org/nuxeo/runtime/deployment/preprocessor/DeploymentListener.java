package org.nuxeo.runtime.deployment.preprocessor;


import java.io.File;
import java.net.URL;

import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

public class DeploymentListener implements BundleListener {

	protected  DeploymentPreprocessor processor;
	
	protected DeploymentListener(DeploymentPreprocessor processor) {
		this.processor = processor;
	}
	
	@Override
	public void bundleChanged(BundleEvent event) {
		if (BundleEvent.STARTING != event.getType()) {
			return;
		}
		Bundle bundle = event.getBundle();
		URL bundleLoc = bundle.getEntry("/");
		File bundleFile = new File(bundleLoc.getPath());
		try {
			processor.processFile(processor.getRootContainer(), bundleFile);
		} catch (Exception e) {
			LogFactory.getLog(DeploymentListener.class).error("Cannot preprocess bundle " + bundle.getSymbolicName(), e);
		}
	}

}
