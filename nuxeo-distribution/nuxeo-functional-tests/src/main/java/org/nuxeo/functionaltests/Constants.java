/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 *
 */

package org.nuxeo.functionaltests;

/**
 * @since 8.2
 */
public class Constants {

    private Constants() {
        // Constants class
    }

    public static final String NXPATH_URL_FORMAT = "/nxpath/default%s@view_documents";

    public static final String NXDOC_URL_FORMAT = "/nxdoc/default/%s/view_documents";

    public static final String SECTIONS_PATH = "/default-domain/sections/";

    public static final String SECTIONS_TITLE = "Sections";

    public static final String WORKSPACES_PATH = "/default-domain/workspaces/";

    public static final String WORKSPACES_TITLE = "Workspaces";

    public static final String WORKSPACES_URL = String.format(NXPATH_URL_FORMAT, WORKSPACES_PATH);

    public static final String WORKSPACE_TYPE = "Workspace";

    public static final String FILE_TYPE = "File";

    public static final String SECTION_TYPE = "Workspace";

    public static final String FOLDER_TYPE = "Folder";

}
