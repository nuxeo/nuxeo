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
package org.nuxeo.targetplatforms.jsf.actions;

import static org.jboss.seam.ScopeType.EVENT;

import java.util.Collections;
import java.util.List;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.targetplatforms.api.TargetPlatformInfo;
import org.nuxeo.targetplatforms.api.service.TargetPlatformService;

/**
 * UI actions for {@link TargetPlatformService}.
 *
 * @since 5.7.1
 */
@Scope(EVENT)
@Name("targetPlatformActions")
public class TargetPlatformActions {

    @In(create = true)
    protected TargetPlatformService targetPlatformService;

    public List<TargetPlatformInfo> getPlatforms() {
        List<TargetPlatformInfo> res = targetPlatformService.getAvailableTargetPlatformsInfo(null);
        Collections.sort(res);
        return res;
    }

}
