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
 *     Arnaud Kervern
 */
package org.nuxeo.runtime.test.runner;

import java.util.HashSet;
import java.util.Set;

/**
 * A TargetExtensions is part of PartialDeploy annotation that is able to deploy only a subset of extensions in a
 * bundle. A TargetExtensions defined which target component and extension point are allowed to be deployed.
 *
 * @since 9.1
 */
public abstract class TargetExtensions {
    protected Set<String> targetExtensions = new HashSet<>();

    protected TargetExtensions() {
        initialize();
    }

    public Set<String> getTargetExtensions() {
        return targetExtensions;
    }

    public void addTargetExtension(String name, String extension) {
        targetExtensions.add(newTargetExtension(name, extension));
    }

    public static String newTargetExtension(String name, String extension) {
        return String.format("%s:%s", name, extension);
    }

    protected abstract void initialize();

    /**
     * White list contributions: TypeService schema and doctype definition, LifecycleService lifecycle and associated
     * types, SQLDirectoryFactory directories and VersioningService versioning rules.
     */
    public static class ContentModel extends TargetExtensions {
        public ContentModel() {
            super();
        }

        @Override
        protected void initialize() {
            addTargetExtension("org.nuxeo.ecm.core.schema.TypeService", "schema");
            addTargetExtension("org.nuxeo.ecm.core.schema.TypeService", "doctype");
            addTargetExtension("org.nuxeo.ecm.core.lifecycle.LifeCycleService", "types");
            addTargetExtension("org.nuxeo.ecm.core.lifecycle.LifeCycleService", "lifecycle");
            addTargetExtension("org.nuxeo.ecm.directory.GenericDirectory", "directories");
            addTargetExtension("org.nuxeo.ecm.core.versioning.VersioningService", "versioningRules");
        }
    }

    /**
     * White list {@link ContentModel} and ContentTemplateService
     */
    public static class ContentTemplate extends ContentModel {
        @Override
        protected void initialize() {
            super.initialize();
            addTargetExtension("org.nuxeo.ecm.platform.content.template.service.ContentTemplateService",
                    "factoryBinding");
        }
    }

    /**
     * White list {@link ContentModel} and Automation related contributions
     */
    public static class Automation extends ContentModel {
        @Override
        protected void initialize() {
            super.initialize();
            addTargetExtension("org.nuxeo.ecm.core.operation.OperationServiceComponent", "event-handlers");
            addTargetExtension("org.nuxeo.ecm.core.operation.OperationServiceComponent", "chains");
            addTargetExtension("org.nuxeo.automation.scripting.internals.AutomationScriptingComponent", "operation");
        }
    }
}
