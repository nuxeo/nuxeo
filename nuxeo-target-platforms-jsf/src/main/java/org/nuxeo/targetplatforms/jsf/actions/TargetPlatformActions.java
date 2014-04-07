/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
import org.nuxeo.ecm.core.api.ClientException;
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

    public List<TargetPlatformInfo> getPlatforms() throws ClientException {
        List<TargetPlatformInfo> res = targetPlatformService.getAvailableTargetPlatformsInfo(null);
        Collections.sort(res);
        return res;
    }

}