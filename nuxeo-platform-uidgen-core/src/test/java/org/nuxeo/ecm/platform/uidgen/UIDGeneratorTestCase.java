package org.nuxeo.ecm.platform.uidgen;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.platform.uidgen.service.ServiceHelper;
import org.nuxeo.ecm.platform.uidgen.service.UIDGeneratorService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features({TransactionalFeature.class, CoreFeature.class})
@Deploy({"org.nuxeo.ecm.core.persistence", "org.nuxeo.ecm.platform.uidgen.core"})
@LocalDeploy("org.nuxeo.ecm.platform.uidgen.core:nxuidgenerator-test-contrib.xml")
public abstract class UIDGeneratorTestCase  {

    @Inject CoreSession session;

    UIDGeneratorService service;

    @Before
    public void lookupService() {
        service = ServiceHelper.getUIDGeneratorService();
    }

}