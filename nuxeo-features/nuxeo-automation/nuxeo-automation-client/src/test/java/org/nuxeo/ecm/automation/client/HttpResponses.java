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
package org.nuxeo.ecm.automation.client;

/**
 * @author dmetzler
 */
public class HttpResponses {

    public static final String DOC_WORKSPACE = "{" + "\"entity-type\": \"documents\"," + "\"entries\": [" + "    {"
            + "        \"entity-type\": \"document\"," + "        \"repository\": \"default\","
            + "        \"uid\": \"1214a215-33c1-42d9-809f-5ca686f1bc9f\","
            + "        \"path\": \"/default-domain/workspaces\"," + "        \"type\": \"WorkspaceRoot\","
            + "        \"state\": \"project\"," + "        \"title\": \"Workspaces\","
            + "        \"lastModified\": \"2012-09-12T14:10:07.49Z\"," + "        \"facets\": ["
            + "            \"SuperSpace\"," + "            \"DocumentsSizeStatistics\"," + "            \"Folderish\","
            + "            \"DocumentsCountStatistics\"" + "        ]," + "        \"changeToken\": \"1347459007491\","
            + "        \"contextParameters\": {}" + "    }" + "]" + "}";

    // Returning a document with no Path (NXP-6777)
    public static final String DOC_NOPATH = "{" + "\"entity-type\": \"documents\"," + "\"entries\": [" + "    {"
            + "        \"entity-type\": \"document\"," + "        \"repository\": \"default\","
            + "        \"uid\": \"1214a215-33c1-42d9-809f-5ca686f1bc9f\"," + "        \"type\": \"WorkspaceRoot\","
            + "        \"state\": \"project\"," + "        \"title\": \"Workspaces\","
            + "        \"lastModified\": \"2012-09-12T14:10:07.49Z\"," + "        \"facets\": ["
            + "            \"SuperSpace\"," + "            \"DocumentsSizeStatistics\"," + "            \"Folderish\","
            + "            \"DocumentsCountStatistics\"" + "        ]," + "        \"changeToken\": \"1347459007491\","
            + "        \"contextParameters\": {}" + "    }" + "]" + "}";

    public static final String DOC_DEFAULT_DOMAIN = "{" + "  \"entity-type\":\"document\","
            + "  \"repository\":\"default\"," + "  \"uid\":\"6e4ee4b8-af3f-4fb4-ad31-1a0a88720dfb\","
            + "  \"path\":\"/default-domain\"," + "  \"type\":\"Domain\"," + "  \"state\":\"project\","
            + "  \"versionLabel\":\"\"," + "  \"title\":\"Domain\","
            + "  \"lastModified\":\"2013-05-16T11:35:00.56Z\"," + "  \"facets\":[\"SuperSpace\",\"Folderish\"],"
            + "  \"changeToken\":\"1368704100560\"," + "  \"contextParameters\":{}" + "}";

    public static final String DOC_LOCK_AND_VERSIONNED = "{"
            + "    \"entity-type\":\"document\","
            + "    \"repository\":\"default\","
            + "    \"uid\":\"8243123c-34d0-4e33-b4b3-290cef008db0\","
            + "    \"path\":\"/default-domain/UserWorkspaces/Administrator/My File\","
            + "    \"type\":\"File\","
            + "    \"state\":\"project\","
            + "    \"versionLabel\":\"1.1\","
            + "    \"isCheckedOut\":false,"
            + "    \"lockOwner\":\"Administrator\","
            + "    \"lockCreated\":\"2013-05-16T17:58:26.618+02:00\","
            + "    \"title\":\"My File\","
            + "    \"lastModified\":\"2013-05-16T15:58:19.00Z\","
            + "    \"facets\":[\"Downloadable\",\"Commentable\",\"Asset\",\"Versionable\",\"Publishable\",\"HasRelatedText\"],"
            + "    \"changeToken\":\"1368719899000\"," + "    \"contextParameters\":{}" + "}";

}
