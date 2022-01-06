/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.core.version.test;

import static org.nuxeo.ecm.core.storage.sql.DatabaseHelper.ID_TYPE_PROPERTY;

import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * @since 2021.15
 */
// NXP-30681
@WithFrameworkProperty(name = ID_TYPE_PROPERTY, value = "sequence")
public class TestVersioningRemovalPolicyWithSequenceId extends TestVersioningRemovalPolicy {

}
