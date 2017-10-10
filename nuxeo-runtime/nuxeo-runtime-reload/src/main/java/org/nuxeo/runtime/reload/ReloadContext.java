/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 *
 */
package org.nuxeo.runtime.reload;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * The reload actions to perform when reloading the Nuxeo server.
 *
 * @since 9.3
 */
public class ReloadContext {

    protected final List<String> bundlesNamesToUndeploy;

    protected final List<File> bundlesToDeploy;

    /** The bundle destination relative path, it will be computed from runtime home (usually nxserver). */
    protected final Path bundlesDestination;

    public ReloadContext() {
        this("bundles");
    }

    /**
     * Constructor which takes the destination as argument. The given path must be relative to nxserver directory.
     */
    public ReloadContext(String bundlesDestination) {
        this(Paths.get(bundlesDestination));
    }

    /**
     * Constructor which takes the destination as argument. The given path must be relative to nxserver directory.
     */
    public ReloadContext(Path bundlesDestination) {
        bundlesNamesToUndeploy = new ArrayList<>();
        bundlesToDeploy = new ArrayList<>();
        this.bundlesDestination = Objects.requireNonNull(bundlesDestination, "Bundles destination must not be null");
    }

    public ReloadContext undeploy(List<String> bundleNames) {
        bundlesNamesToUndeploy.addAll(bundleNames);
        return this;
    }

    public ReloadContext undeploy(String... bundleNames) {
        return undeploy(Arrays.asList(bundleNames));
    }

    public ReloadContext deploy(List<File> bundleFiles) {
        bundlesToDeploy.addAll(bundleFiles);
        return this;
    }

    public ReloadContext deploy(File... bundleFiles) {
        return deploy(Arrays.asList(bundleFiles));
    }

}
