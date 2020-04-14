/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core;

import org.nuxeo.ecm.core.test.FulltextSearchDisabledFeature;
import org.nuxeo.runtime.test.runner.Features;

@Features(FulltextSearchDisabledFeature.class)
public class TestFulltextSearchDisabledNoQuery extends TestFulltextAbstractNoQuery {

    @Override
    protected boolean expectBinaryText() {
        // binary text available when only fulltext search is disabled
        return true;
    }

}
