package org.nuxeo.ecm.platform.routing.test;

import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.SimpleFeature;

@Features({ TransactionalFeature.class, CoreFeature.class, AutomationFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.content.template", //
        "org.nuxeo.ecm.automation.core", //
        "org.nuxeo.ecm.directory", //
        "org.nuxeo.ecm.platform.usermanager", //
        "org.nuxeo.ecm.directory.types.contrib", //
        "org.nuxeo.ecm.directory.sql", //
        "org.nuxeo.ecm.platform.userworkspace.core", //
        "org.nuxeo.ecm.platform.userworkspace.types", //
        "org.nuxeo.ecm.platform.task.api", //
        "org.nuxeo.ecm.platform.task.core", //
        "org.nuxeo.ecm.platform.routing.api", "org.nuxeo.ecm.platform.routing.core" //
})
@LocalDeploy({ "org.nuxeo.ecm.platform.routing.core:OSGI-INF/test-sql-directories-contrib.xml",
        "org.nuxeo.ecm.platform.routing.core:OSGI-INF/test-graph-operations-contrib.xml",
        "org.nuxeo.ecm.platform.routing.core:OSGI-INF/test-graph-types-contrib.xml" })
public class WorkflowFeature extends SimpleFeature {

}
