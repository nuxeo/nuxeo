package org.nuxeo.ecm.platform.login.deputy.management.tests;


import org.nuxeo.runtime.api.ConnectionHelper;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.SimpleFeature;

@LocalDeploy(
        "org.nuxeo.runtime.datasource:ds-contrib.xml")
public class SingleDatasourceFeature extends SimpleFeature {

	protected String savedSingleDS;

	@Override
	public void start(FeaturesRunner runner) throws Exception {
		savedSingleDS = System.getProperty(ConnectionHelper.SINGLE_DS);
		System.setProperty(ConnectionHelper.SINGLE_DS, "jdbc/NuxeoTestDS");
	}

	
	@Override
	public void stop(FeaturesRunner runner) throws Exception {
		if (savedSingleDS == null || savedSingleDS.isEmpty()) {
			System.clearProperty(ConnectionHelper.SINGLE_DS);
		} else {
			System.setProperty(ConnectionHelper.SINGLE_DS, savedSingleDS);
		}
	}
	
}
