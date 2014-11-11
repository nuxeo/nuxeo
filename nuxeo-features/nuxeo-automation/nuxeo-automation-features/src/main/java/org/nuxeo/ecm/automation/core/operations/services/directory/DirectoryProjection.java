/*
 * (C) Copyright 2006-2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     ldoguin
 *
 */
package org.nuxeo.ecm.automation.core.operations.services.directory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;

/**
 * @author Laurent Doguin (ldoguin@nuxeo.com)
 * @since 5.7.2
 */
@Operation(id = DirectoryProjection.ID, category = Constants.CAT_SERVICES, label = "Get a Directory Projection", since = "5.7.2", description = "Executes a query using given filter and return only the column *<b>columnName</b>*. The result is assigned to the context variable *<b>variableName</b>*. The filters are specified as <i>key=value</i> pairs separated by a new line. The key used for a filter is the column name of the directory. To specify multi-line values you can use a \\ character followed by a new line. <p>Example:<pre>firstName=John<br>lastName=doe</pre>By default, the search filters use exact match. You can do a fulltext search on some specific columns using the fulltextFields. it's specified as comma separated columnName, for instance : <p>Example:<pre>firstName,lastName</pre>", addToStudio=false)
public class DirectoryProjection {

    public static final String ID = "Directory.Projection";

    private static final Log log = LogFactory.getLog(DirectoryProjection.class);

    @Context
    protected OperationContext ctx;

    @Context
    protected DirectoryService directoryService;

    @Param(name = "directoryName", required = true)
    protected String directoryName;

    @Param(name = "columnName", required = true)
    protected String columnName;

    @Param(name = "variableName", required = true)
    protected String variableName;

    @Param(name = "filters", required = false)
    protected Properties filterProperties;

    @Param(name = "fulltextFields", required = false)
    protected StringList fulltextFields;

    @OperationMethod
    public void run() throws Exception {
        Directory directory = directoryService.getDirectory(directoryName);
        Session session = null;
        try {
            session = directory.getSession();
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            Set<String> fulltext = new HashSet<String>();
            if (filterProperties != null) {
                filter.putAll(filterProperties);
                if (fulltextFields != null) {
                    fulltext.addAll(fulltextFields);
                }
            }
            List<String> uids = session.getProjection(filter, fulltext,
                    columnName);
            ctx.put(variableName, uids);
        } finally {
            try {
                if (session != null) {
                    session.close();
                }
            } catch (ClientException ce) {
                log.error("Could not close directory session", ce);
            }
        }
    }

}
