/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.smart.folder.jsf;

/**
 * Constants for a SmartFolder document instance, needed to initialize this document type from a global smart search.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public interface SmartFolderDocumentConstants {

    String QUERY_PART_PROP_NAME = "sf:queryPart";

    String SELECTED_LAYOUT_COLUMNS_PROP_NAME = "cvd:selectedLayoutColumns";

    String SORT_INFOS_PROP_NAME = "cvd:sortInfos";

    String PAGE_SIZE_PROP_NAME = "cvd:pageSize";

}
