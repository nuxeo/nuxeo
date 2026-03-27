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
 *     Thomas Roger <thomas.roger@hyland.com>
 */
package org.nuxeo.ecm.platform.web.common;

import org.nuxeo.ecm.core.event.CoreEventFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.RunnerFeature;

/**
 * Test feature that deploys the {@code nuxeo-platform-web-common} bundle.
 *
 * @since 2025.18
 */
@Deploy("org.nuxeo.ecm.platform.web.common")
@Features(CoreEventFeature.class)
public class WebCommonFeature implements RunnerFeature {
}
