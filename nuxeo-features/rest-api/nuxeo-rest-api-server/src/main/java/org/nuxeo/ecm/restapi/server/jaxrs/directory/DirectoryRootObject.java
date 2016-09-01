/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.server.jaxrs.directory;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7.8
 */
@WebObject(type = "directory")
public class DirectoryRootObject extends DefaultObject {

    @Path("{directoryName}")
    public Object doGetDirectory(@PathParam("directoryName") String dirName) {
        return newObject("directoryObject", dirName);
    }

    /**
     * Get non system directory list.
     *
     * @param types if not empty, returns only the directories having at least one of the requested type
     * @return the list of directories that are not system directories
     * @since 8.4
     */
    @GET
    public List<Directory> getDirectoryNames(@QueryParam("types") List<String> types) {
        DirectoryService directoryService = Framework.getLocalService(DirectoryService.class);
        List<Directory> result = new ArrayList<Directory>();
        for (Directory dir : directoryService.getDirectories()) {
            if (dir.getTypes().contains(DirectoryService.SYSTEM_DIRECTORY_TYPE)) {
                continue;
            } else if (types == null || types.isEmpty()) {
                result.add(dir);
            } else {
                if (types.stream().filter(dir.getTypes()::contains).findFirst().isPresent()) {
                    result.add(dir);
                }
            }
        }
        return result;
    }

}
