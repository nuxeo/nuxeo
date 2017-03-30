/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.connect.tools.report.web.jgiven;

import com.tngtech.jgiven.annotation.ExpectedScenarioState;
import com.tngtech.jgiven.impl.ScenarioExecutor;

public class Stage<SELF extends Stage<SELF>> extends com.tngtech.jgiven.Stage<SELF> {

    @ExpectedScenarioState
    protected ScenarioExecutor executor;

    public Stage() {
        super();
    }

    public <S> S and(Class<S> typeof) {
        return executor.addStage(typeof);
    }

    public <S> S given(Class<S> typeof) {
        return executor.addStage(typeof);
    }

    public <S> S when(Class<S> typeof) {
        return executor.addStage(typeof);
    }

    public <S> S then(Class<S> typeof) {
        return executor.addStage(typeof);
    }
}