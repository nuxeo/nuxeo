package org.nuxeo.ecm.platform.publisher.test;

import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.annotations.repository.AbstractRepositoryTestCase;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Deploy({"org.nuxeo.ecm.platform.publisher.core.contrib","org.nuxeo.ecm.platform.publisher.core"})
public abstract class PublisherTestCase extends AbstractRepositoryTestCase {



}