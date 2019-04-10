/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Mariana Cedica <mcedica@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.elasticsearch;

import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;
import org.nuxeo.drive.service.AuditChangeFinderClusteringEnabledTestSuite;
import org.nuxeo.drive.service.AuditChangeFinderTestSuite;
import org.nuxeo.drive.test.ESAuditFeature;
import org.nuxeo.runtime.test.runner.ContributableFeaturesRunner;
import org.nuxeo.runtime.test.runner.Features;

/**
 * Runs the {@link AuditChangeFinderTestSuite} using the {@link ESAuditChangeFinder}.
 *
 * @since 7.3
 */
@RunWith(ContributableFeaturesRunner.class)
@Features({ ESAuditFeature.class })
@SuiteClasses({ AuditChangeFinderTestSuite.class, AuditChangeFinderClusteringEnabledTestSuite.class})
public class TestESAuditChangeFinder {


}
