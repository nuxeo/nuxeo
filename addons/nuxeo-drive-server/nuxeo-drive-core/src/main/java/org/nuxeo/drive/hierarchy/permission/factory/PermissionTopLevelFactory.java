/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.hierarchy.permission.adapter.PermissionTopLevelFolderItem;
import org.nuxeo.drive.service.TopLevelFolderItemFactory;
import org.nuxeo.drive.service.impl.AbstractVirtualFolderItemFactory;
import org.nuxeo.ecm.core.api.ClientException;

/**
 * User workspace and permission based implementation of the {@link TopLevelFolderItemFactory}.
 * 
 * @author Antoine Taillefer
 */
public class PermissionTopLevelFactory extends AbstractVirtualFolderItemFactory implements TopLevelFolderItemFactory {

    private static final Log log = LogFactory.getLog(PermissionTopLevelFactory.class);

    protected static final String CHILDREN_FACTORIES_PARAM = "childrenFactories";

    protected List<String> childrenFactoryNames = new ArrayList<String>();

    /*---------------------- FileSystemItemFactory ---------------*/
    @Override
    public void handleParameters(Map<String, String> parameters) throws ClientException {
        super.handleParameters(parameters);
        // Look for the "childrenFactories" parameter
        String childrenFactoriesParam = parameters.get(CHILDREN_FACTORIES_PARAM);
        if (!StringUtils.isEmpty(childrenFactoriesParam)) {
            childrenFactoryNames.addAll(Arrays.asList(childrenFactoriesParam.split(",")));
        } else {
            log.warn(String.format(
                    "Factory %s has no %s parameter, please provide one in the factory contribution using a comma separated list to set the children factory names.",
                    getName(), CHILDREN_FACTORIES_PARAM));
        }
    }

    /*---------------------- VirtualFolderItemFactory ---------------*/
    @Override
    public FolderItem getVirtualFolderItem(Principal principal) throws ClientException {
        return getTopLevelFolderItem(principal);
    }

    /*----------------------- TopLevelFolderItemFactory ---------------------*/
    @Override
    public FolderItem getTopLevelFolderItem(Principal principal) throws ClientException {
        return new PermissionTopLevelFolderItem(getName(), principal, getFolderName(), childrenFactoryNames);
    }

}
