package org.nuxeo.ecm.core.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hamcrest.Matchers;
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class CanRenameDuplicateTest {

    Log log = LogFactory.getLog(CanRenameDuplicateTest.class);

    @Inject
    CoreSession repo;

    @Test
    public void duplicateAreRenamed() throws ClientException {
        DocumentModel model = repo.createDocumentModel("/", "aFile", "File");

        DocumentModel original = repo.createDocument(model);
        String originalName = original.getName();
        Assert.assertThat(originalName, Matchers.is("aFile"));

        DocumentModel duplicate = repo.createDocument(model);
        String duplicateName = duplicate.getName();
        Assert.assertThat(duplicateName, Matchers.startsWith("aFile."));
    }

    @Test
    public void profileUnderLoad() throws ClientException {
        Assume.assumeTrue(Boolean.parseBoolean(Framework.getProperty("profile", "false")));
        SimonManager.enable();
        try {
            Stopwatch watch = SimonManager.getStopwatch("test.profile");
            DocumentModel model = repo.createDocumentModel("File");
            for (int i = 1; i <= 10000; ++i) {
                String increment = String.format("%05d", i);
                model.setPathInfo("/", "aFile-" + increment);
                Split split = watch.start();
                repo.createDocument(model);
                split.stop();
            }
            log.info(watch);
        } finally {
            SimonManager.disable();
        }
    }

    @Inject
    EventServiceAdmin admin;

    @Test
    public void profileUnderLoadWithoutDuplicateChecker()
            throws ClientException {
        Assume.assumeTrue(Boolean.parseBoolean(Framework.getProperty("profile", "false")));
        admin.setListenerEnabledFlag("duplicatedNameFixer", false);
        profileUnderLoad();
    }
}
