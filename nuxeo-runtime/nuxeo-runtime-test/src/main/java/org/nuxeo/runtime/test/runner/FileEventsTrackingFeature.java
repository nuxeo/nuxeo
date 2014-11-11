package org.nuxeo.runtime.test.runner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.common.Environment;
import org.nuxeo.runtime.trackers.concurrent.ThreadEvent;
import org.nuxeo.runtime.trackers.files.FileEvent;
import org.nuxeo.runtime.trackers.files.FileEventHandler;
import org.nuxeo.runtime.trackers.files.FileEventListener;

@Features(RuntimeFeature.class)
public class FileEventsTrackingFeature extends SimpleFeature {

    protected class Tracker implements FileEventHandler {

        @Override
        public void onFile(File file, Object marker) {
            tracked.add(file);
        }

    }

    protected final Set<File> tracked = new HashSet<>();

    protected Tracker tracker = new Tracker();

    protected FileEventListener listener = new FileEventListener(tracker);

    protected Path tempPath;

    protected Set<File> created = new HashSet<File>();

    @Override
    public void beforeMethodRun(FeaturesRunner runner, FrameworkMethod method,
            Object test) throws Exception {
        File temp = Environment.getDefault().getTemp();
        temp.mkdirs();
        tempPath = temp.toPath();
        tracked.clear();
        created.clear();
        listener.install();
    }

    @Override
    public void afterMethodRun(FeaturesRunner runner, FrameworkMethod method,
            Object test) throws Exception {
        listener.uninstall();
        try {
            Assert.assertThat(tracked, CoreMatchers.is(created)); // replace
                                                                  // with
                                                                  // contains
            for (File each : created) {
                Assert.assertThat(each.exists(), CoreMatchers.is(false));
            }
        } finally {
            tracked.clear();
            created.clear();
        }
    }

    public ThreadEvent onThreadEnter(boolean isLongRunning) {
        return ThreadEvent.onEnter(this, isLongRunning);
    }

    public FileEvent onFile(File aFile, Object aMarker) {
        return FileEvent.onFile(this, resolveAndCreate(aFile), aMarker);
    }

    public File resolveAndCreate(File aFile) {
        File temp = Environment.getDefault().getTemp();
        File actual = temp.toPath().resolve(aFile.toPath()).toFile();
        try {
            actual.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException("Cannot create temp file " + actual);
        }
        created.add(actual);
        return actual;
    }
}
