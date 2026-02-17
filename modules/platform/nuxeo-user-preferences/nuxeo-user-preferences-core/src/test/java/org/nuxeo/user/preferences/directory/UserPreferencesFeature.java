/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.user.preferences.directory;

import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.user.preferences.stream.StreamUserDocPreferencesGC;

/**
 * @since 2025.16
 */
@Deploy("org.nuxeo.ecm.platform.htmlsanitizer")
@Deploy("org.nuxeo.platform.user.preferences.api")
@Deploy("org.nuxeo.platform.user.preferences.core")
@Features(PlatformFeature.class)
public class UserPreferencesFeature implements RunnerFeature {

    @Override
    public void initialize(FeaturesRunner runner) {
        runner.getFeature(TransactionalFeature.class)
              .addWaiter(
                      duration -> Framework.getService(StreamService.class)
                                           .await(Name.ofUrn(StreamUserDocPreferencesGC.STREAM_NAME),
                                                   Name.ofUrn(StreamUserDocPreferencesGC.COMPUTATION_NAME), duration));
    }
}
