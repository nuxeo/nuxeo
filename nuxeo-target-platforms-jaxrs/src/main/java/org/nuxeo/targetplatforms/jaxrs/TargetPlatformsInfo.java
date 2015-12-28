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
package org.nuxeo.targetplatforms.jaxrs;

import java.util.ArrayList;
import java.util.Collection;

import org.nuxeo.targetplatforms.api.TargetPlatformInfo;

/**
 * @since 5.9.3
 */
public class TargetPlatformsInfo extends ArrayList<TargetPlatformInfo> {

    private static final long serialVersionUID = 1L;

    public TargetPlatformsInfo() {
        super();
    }

    public TargetPlatformsInfo(Collection<? extends TargetPlatformInfo> c) {
        super(c);
    }

}
