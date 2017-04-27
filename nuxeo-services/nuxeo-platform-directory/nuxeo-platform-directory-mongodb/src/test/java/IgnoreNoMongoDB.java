/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

import javax.inject.Inject;

import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule;

/**
 * @since 9.1
 */
public final class IgnoreNoMongoDB implements ConditionalIgnoreRule.Condition {

    @Inject
    protected CoreFeature coreFeature;

    public boolean shouldIgnore() {
        return !coreFeature.getStorageConfiguration().isDBSMongoDB();
    }

}