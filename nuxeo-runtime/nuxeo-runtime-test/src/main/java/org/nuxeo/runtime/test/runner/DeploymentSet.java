/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.runtime.test.runner;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DeploymentSet {

    protected final LinkedHashSet<String> deployments;
    protected final LinkedHashSet<String> localDeployments;

    public DeploymentSet() {
        deployments = new LinkedHashSet<String>();
        localDeployments = new LinkedHashSet<String>();
    }

    public void load(AnnotationScanner scanner, Class<?> clazz) {
        scanner.scan(clazz);
        List<? extends Annotation> annos = scanner.getAnnotations(clazz);
        for (Annotation anno : annos) {
            if (anno.annotationType() == Deploy.class) {
                for (String key : ((Deploy) anno).value()) {
                    deployments.add(key);
                }
            } else if (anno.annotationType() == LocalDeploy.class) {
                for (String key : ((LocalDeploy) anno).value()) {
                    localDeployments.add(key);
                }
            }
        }
    }

    public void addDeployment(String key) {
        deployments.add(key);
    }

    public void addLocalDeployment(String key) {
        localDeployments.add(key);
    }

    public void addDeployment(Collection<String> key) {
        deployments.addAll(key);
    }

    public void addLocalDeployment(Collection<String> key) {
        localDeployments.addAll(key);
    }

    public Set<String> getDeployments() {
        return deployments;
    }

    public Set<String> getLocalDeployments() {
        return localDeployments;
    }

}
