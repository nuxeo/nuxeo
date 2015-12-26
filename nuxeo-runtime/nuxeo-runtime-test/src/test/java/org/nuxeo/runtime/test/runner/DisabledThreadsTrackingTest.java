/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.test.runner;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.trackers.concurrent.ThreadEvent;

@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, FileEventsTrackingFeature.class })
@BlacklistComponent("org.nuxeo.runtime.trackers.files.threadstracking.config")
public class DisabledThreadsTrackingTest {

    @Inject
    protected FeaturesRunner runner = null;

    protected FileEventsTrackingFeature feature;

    @Before
    public void injectTransientFilesFeature() {
        feature = runner.getFeature(FileEventsTrackingFeature.class);
    }

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void filesAreDeletedWhenThreadLeave() throws IOException {
        thrown.handleAssertionErrors();
        thrown.expect(AssertionError.class);
        ThreadEvent.onEnter(this, false).send();
        try {
            feature.onFile(new File("pfouh"), this).send();
        } finally {
            ThreadEvent.onLeave(this).send();
        }
    }

}
