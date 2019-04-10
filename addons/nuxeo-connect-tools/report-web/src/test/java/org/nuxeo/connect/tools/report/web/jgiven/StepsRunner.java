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

import java.util.List;
import java.util.function.Consumer;

import org.junit.internal.AssumptionViolatedException;
import org.junit.runners.model.MultipleFailureException;
import com.google.common.collect.Lists;
import com.tngtech.jgiven.impl.ScenarioBase;
import com.tngtech.jgiven.junit.ScenarioModelHolder;

public class StepsRunner<SELF extends StepsRunner<SELF>> {

    protected final ScenarioBase scenario = new ScenarioBase();

    public StepsRunner<SELF> with(Class<?> model) {
        scenario.setModel(ScenarioModelHolder.getInstance().getReportModel(model));
        return this;
    }

    public <T> StepsRunner<SELF> run(Class<T> stepsClass, String description, Consumer<T> runner) throws Throwable {
        scenario.startScenario(description);
        try {
            runner.accept(scenario.addStage(stepsClass));
            succeeded();
        } catch (AssumptionViolatedException error) {
            throw error;
        } catch (Throwable error) {
            failed(error);
            throw error;
        }
        return this;
    }

    protected void succeeded() throws Throwable {
        scenario.finished();
    }

    protected void failed(Throwable error) {
        if (scenario.getExecutor().hasFailed()) {
            Throwable failedException = scenario.getExecutor().getFailedException();
            List<Throwable> errors = Lists.newArrayList(failedException, error);
            scenario.getExecutor().setFailedException(new MultipleFailureException(errors));
        } else {
            scenario.getExecutor().failed(error);
        }
        try {
            scenario.finished();
        } catch (Throwable suppressed) {
            error.addSuppressed(suppressed);
        }
    }
}