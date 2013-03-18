/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.client;

/**
 * @author dmetzler
 *
 */
public class HttpResponses {


    public static final String DOC_WORKSPACE = "{"
            + "\"entity-type\": \"documents\","
            + "\"entries\": ["
            + "    {"
            + "        \"entity-type\": \"document\","
            + "        \"repository\": \"default\","
            + "        \"uid\": \"1214a215-33c1-42d9-809f-5ca686f1bc9f\","
            + "        \"path\": \"/default-domain/workspaces\","
            + "        \"type\": \"WorkspaceRoot\","
            + "        \"state\": \"project\","
            + "        \"title\": \"Workspaces\","
            + "        \"lastModified\": \"2012-09-12T14:10:07.49Z\","
            + "        \"facets\": ["
            + "            \"SuperSpace\","
            + "            \"DocumentsSizeStatistics\","
            + "            \"Folderish\","
            + "            \"DocumentsCountStatistics\""
            + "        ],"
            + "        \"changeToken\": \"1347459007491\","
            + "        \"contextParameters\": {}"
            + "    }"
            + "]"
            + "}";

    //Returning a document with no Path (NXP-6777)
    public static final String DOC_NOPATH = "{"
            + "\"entity-type\": \"documents\","
            + "\"entries\": ["
            + "    {"
            + "        \"entity-type\": \"document\","
            + "        \"repository\": \"default\","
            + "        \"uid\": \"1214a215-33c1-42d9-809f-5ca686f1bc9f\","
            + "        \"type\": \"WorkspaceRoot\","
            + "        \"state\": \"project\","
            + "        \"title\": \"Workspaces\","
            + "        \"lastModified\": \"2012-09-12T14:10:07.49Z\","
            + "        \"facets\": ["
            + "            \"SuperSpace\","
            + "            \"DocumentsSizeStatistics\","
            + "            \"Folderish\","
            + "            \"DocumentsCountStatistics\""
            + "        ],"
            + "        \"changeToken\": \"1347459007491\","
            + "        \"contextParameters\": {}"
            + "    }"
            + "]"
            + "}";


}
