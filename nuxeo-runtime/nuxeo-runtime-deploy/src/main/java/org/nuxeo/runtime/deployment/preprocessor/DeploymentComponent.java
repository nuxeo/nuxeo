package org.nuxeo.runtime.deployment.preprocessor;

import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

public class DeploymentComponent extends DefaultComponent {

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        DeploymentActivator.me.preprocessor.predeploy();
    }

}
