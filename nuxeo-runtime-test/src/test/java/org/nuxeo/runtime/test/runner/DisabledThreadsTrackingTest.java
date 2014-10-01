package org.nuxeo.runtime.test.runner;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
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

    @Test(expected=AssertionError.class)
    public void filesAreDeletedWhenThreadLeave() throws IOException {
        ThreadEvent.onEnter(this, false).send();
        try {
            feature.onFile(new File("pfouh"), this).send();
        } finally {
            ThreadEvent.onLeave(this).send();
        }
    }

}
