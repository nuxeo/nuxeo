/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.ecm.client.commands;

import java.net.URL;
import java.util.List;

import org.nuxeo.ecm.client.QueryEntry;

/**
 * @author matic
 *
 */
public class GetQueriesCommand extends AbstractCommand<List<QueryEntry>> {

    public final String repositoryId;

    public final String href;

    public GetQueriesCommand(String repositoryId, String href) {
        super("repository", "query");
        this.repositoryId = repositoryId;
        this.href = href;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public String formatURL(URL baseURL) {
        return href;
    }
}
