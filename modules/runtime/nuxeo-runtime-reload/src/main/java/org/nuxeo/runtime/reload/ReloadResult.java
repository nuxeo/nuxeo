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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;

/**
 * Result of hot reload operation.
 *
 * @since 9.3
 */
public class ReloadResult {

    /** Be aware that theses bundles have been uninstalled, some methods may not work. */
    protected final List<Bundle> undeployedBundles;

    protected final List<Bundle> deployedBundles;

    public ReloadResult() {
        undeployedBundles = new ArrayList<>();
        deployedBundles = new ArrayList<>();
    }

    public List<Bundle> undeployedBundles() {
        return undeployedBundles;
    }

    /**
     * @since 10.3
     */
    public Stream<Bundle> undeployedBundlesAsStream() {
        return undeployedBundles().stream();
    }

    public List<Bundle> deployedBundles() {
        return deployedBundles;
    }

    public Stream<Bundle> deployedBundlesAsStream() {
        return deployedBundles().stream();
    }

    public Stream<File> deployedFilesAsStream() {
        return deployedBundlesAsStream().map(Bundle::getLocation).map(File::new);
    }

    public ReloadResult merge(ReloadResult result) {
        undeployedBundles.addAll(result.undeployedBundles);
        deployedBundles.addAll(result.deployedBundles);
        return this;
    }

}
