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
 *     Anahide Tchertchian
 */
package org.nuxeo.runtime.registry;

import javax.inject.Inject;

import org.nuxeo.runtime.test.runner.HotDeployer;

/**
 * Additional tests for different way of hot reloading contributions.
 *
 * @since TODO
 */
public class TestRegistryHotDeployer extends TestRegistry {

    @Inject
    protected HotDeployer hotDeployer;

    @Override
    protected boolean useHotDeployer() {
        return true;
    }

    @Override
    protected void hotDeploy(boolean doDeploy, String contrib) throws Exception {
        String path = "org.nuxeo.runtime.test.tests:" + contrib;
        if (doDeploy) {
            hotDeployer.deploy(path);
        } else {
            hotDeployer.undeploy(path);
        }
    }

}
