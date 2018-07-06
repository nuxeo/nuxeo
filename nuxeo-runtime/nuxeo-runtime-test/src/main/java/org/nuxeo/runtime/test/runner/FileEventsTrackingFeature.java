/*
 * (C) Copyright 2014-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
public class FileEventsTrackingFeature implements RunnerFeature {

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
    public void beforeMethodRun(FeaturesRunner runner, FrameworkMethod method, Object test) throws Exception {
        File temp = Environment.getDefault().getTemp();
        tempPath = temp.toPath();
        tracked.clear();
        created.clear();
        listener.install();
    }

    @Override
    public void afterMethodRun(FeaturesRunner runner, FrameworkMethod method, Object test) throws Exception {
        listener.uninstall();
        try {
            Assert.assertThat(tracked, CoreMatchers.is(created)); // replace
                                                                  // with
                                                                  // contains
            for (File each : created) {
                Assert.assertThat("File should have been deleted: " + each,
                        each.exists(), CoreMatchers.is(false));
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
