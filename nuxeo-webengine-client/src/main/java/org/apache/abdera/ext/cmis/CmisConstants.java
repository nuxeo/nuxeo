/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  The ASF licenses this file to You
* under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.  For additional information regarding
* copyright in this work, please see the NOTICE file in the top level
* directory of this distribution.
*/
package org.apache.abdera.ext.cmis;

import javax.xml.namespace.QName;

public class CmisConstants {

    // Namespaces
    public static final String CMIS_NS = "http://www.cmis.org/2008/05";

    // QNames
    public static final QName QNAME_CMIS_OBJECT = new QName(CMIS_NS, "object");
    public static final QName QNAME_CMIS_PROPERTIES = new QName(CMIS_NS, "properties");
    public static final QName QNAME_CMIS_PROPERTY = new QName(CMIS_NS, "property");
    public static final QName QNAME_CMIS_QUERY = new QName(CMIS_NS, "query");
    public static final QName QNAME_CMIS_REPOSITORY_INFO = new QName(CMIS_NS, "repositoryInfo");

    // URLs
    public static final String UNFILED_COLLECTION = "unfiled";
    public static final String ROOT_CHILDREN_COLLECTION = "root-children";

    // MIME-types
    public static final String MIME_TYPE_ATOM = "application/atom+xml";
    public static final String MIME_TYPE_ATOM_FEED = "application/atom+xml;charset=utf-8;type=feed";
    public static final String MIME_TYPE_ATOM_ENTRY = "application/atom+xml;charset=utf-8;type=entry";
    public static final String MIME_TYPE_CMIS_QUERY = "application/cmisrequest+xml;type=query";

}
