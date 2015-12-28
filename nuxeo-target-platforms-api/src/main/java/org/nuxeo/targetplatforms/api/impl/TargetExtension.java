/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.targetplatforms.api.impl;

import org.nuxeo.targetplatforms.api.Target;

/**
 * {@link Target} implementation relying on an original implementation, useful for override when adding additional
 * metadata.
 *
 * @since 5.7.1
 */
public class TargetExtension extends TargetInfoExtension implements Target {

    private static final long serialVersionUID = 1L;

    protected Target origTarget;

    // needed by GWT serialization
    protected TargetExtension() {
        super();
    }

    public TargetExtension(Target orig) {
        super(orig);
        origTarget = orig;
    }

    @Override
    public boolean isAfterVersion(String version) {
        return origTarget.isAfterVersion(version);
    }

    @Override
    public boolean isStrictlyBeforeVersion(String version) {
        return origTarget.isStrictlyBeforeVersion(version);
    }

    @Override
    public boolean isVersion(String version) {
        return origTarget.isVersion(version);
    }

    @Override
    public boolean isStrictlyBeforeVersion(Target version) {
        return origTarget.isStrictlyBeforeVersion(version);
    }

    @Override
    public boolean isAfterVersion(Target version) {
        return origTarget.isAfterVersion(version);
    }

    @Override
    public boolean isVersion(Target version) {
        return origTarget.isAfterVersion(version);
    }

}
