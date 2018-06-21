/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Stephane Lacoin
 */
package org.nuxeo.ecm.core.test;

import javax.inject.Inject;

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
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class CanRenameDuplicateTest {

    Log log = LogFactory.getLog(CanRenameDuplicateTest.class);

    @Inject
    CoreSession repo;

    @Test
    public void duplicateAreRenamed() {
        DocumentModel model = repo.createDocumentModel("/", "aFile", "File");

        DocumentModel original = repo.createDocument(model);
        String originalName = original.getName();
        Assert.assertThat(originalName, Matchers.is("aFile"));

        DocumentModel duplicate = repo.createDocument(model);
        String duplicateName = duplicate.getName();
        Assert.assertThat(duplicateName, Matchers.startsWith("aFile."));
    }

    @Test
    public void duplicateCheckCanBeSkipped() {
        DocumentModel model = repo.createDocumentModel("/", "aFile", "File");

        DocumentModel original = repo.createDocument(model);
        String originalName = original.getName();
        Assert.assertThat(originalName, Matchers.is("aFile"));

        // this is interesting for performance reason during mass import for instance
        model.putContextData(CoreSession.SKIP_DESTINATION_CHECK_ON_CREATE, true);
        DocumentModel duplicate = repo.createDocument(model);
        String duplicateName = duplicate.getName();
        Assert.assertThat(duplicateName, Matchers.is("aFile"));

        // save now to avoid a ConcurrentUpdateException at tearDown
        try {
            repo.save();
        } catch (ConcurrentUpdateException e) {
            // low-level duplicates are disabled (through unique indexes or constraints)
            TransactionHelper.setTransactionRollbackOnly();
        }
    }

    @Test
    public void profileUnderLoad() {
        Assume.assumeTrue(Boolean.parseBoolean(Framework.getProperty("profile", "false")));
        SimonManager.enable();
        try {
            Stopwatch watch = SimonManager.getStopwatch("test.profile");
            DocumentModel model = repo.createDocumentModel("Document");
            for (int i = 1; i <= 30000; ++i) {
                String increment = String.format("%05d", i);
                model.setPathInfo("/", "aFile-" + increment);
                Split split = watch.start();
                repo.createDocument(model);
            }
            log.info(watch);
        } finally {
            SimonManager.disable();
        }
    }

    @Inject
    EventServiceAdmin admin;

    @Test
    public void profileUnderLoadWithoutDuplicateChecker() {
        Assume.assumeTrue(Boolean.parseBoolean(Framework.getProperty("profile", "false")));
        admin.setListenerEnabledFlag("duplicatedNameFixer", false);
        profileUnderLoad();
    }
}
