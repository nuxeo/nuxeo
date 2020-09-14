/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service;

import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;
import org.nuxeo.drive.fixtures.AbstractChangeFinderTestCase;
import org.nuxeo.drive.fixtures.AuditChangeFinderTestSuite;
import org.nuxeo.drive.fixtures.GroupChangesTestSuite;
import org.nuxeo.drive.service.impl.AuditChangeFinder;
import org.nuxeo.runtime.test.runner.ContributableFeaturesRunner;

/**
 * Runs the {@link AbstractChangeFinderTestCase} implementations using the {@link AuditChangeFinder}.
 *
 * @since 8.2
 */
@RunWith(ContributableFeaturesRunner.class)
@SuiteClasses({ AuditChangeFinderTestSuite.class, GroupChangesTestSuite.class })
public class TestSQLAuditChangeFinder {

}
