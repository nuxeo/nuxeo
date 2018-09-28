/*
 * (C) Copyright 2012-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.hierarchy.permission.factory;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.hierarchy.permission.adapter.PermissionTopLevelFolderItem;
import org.nuxeo.drive.service.TopLevelFolderItemFactory;
import org.nuxeo.drive.service.impl.AbstractVirtualFolderItemFactory;

/**
 * User workspace and permission based implementation of the {@link TopLevelFolderItemFactory}.
 *
 * @author Antoine Taillefer
 */
public class PermissionTopLevelFactory extends AbstractVirtualFolderItemFactory implements TopLevelFolderItemFactory {

    private static final Logger log = LogManager.getLogger(PermissionTopLevelFactory.class);

    protected static final String CHILDREN_FACTORIES_PARAM = "childrenFactories";

    protected List<String> childrenFactoryNames = new ArrayList<>();

    /*---------------------- FileSystemItemFactory ---------------*/
    @Override
    public void handleParameters(Map<String, String> parameters) {
        super.handleParameters(parameters);
        // Look for the "childrenFactories" parameter
        String childrenFactoriesParam = parameters.get(CHILDREN_FACTORIES_PARAM);
        if (!StringUtils.isEmpty(childrenFactoriesParam)) {
            childrenFactoryNames.addAll(Arrays.asList(childrenFactoriesParam.split(",")));
        } else {
            log.warn(
                    "Factory {} has no {} parameter, please provide one in the factory contribution using a comma separated list to set the children factory names.",
                    this::getName, () -> CHILDREN_FACTORIES_PARAM);
        }
    }

    /*---------------------- VirtualFolderItemFactory ---------------*/
    @Override
    public FolderItem getVirtualFolderItem(Principal principal) {
        return getTopLevelFolderItem(principal);
    }

    /*----------------------- TopLevelFolderItemFactory ---------------------*/
    @Override
    public FolderItem getTopLevelFolderItem(Principal principal) {
        return new PermissionTopLevelFolderItem(getName(), principal, getFolderName(), childrenFactoryNames);
    }

}
