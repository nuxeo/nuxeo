/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Luís Duarte
 */
package org.nuxeo.ecm.directory.providers;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.query.sql.model.OrderByExpr;
import org.nuxeo.ecm.core.query.sql.model.OrderByExprs;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryEntry;
import org.nuxeo.ecm.platform.query.api.AbstractPageProvider;

/**
 * Simple page provider giving the possibility to paginate a {@link Directory}.
 *
 * @since 10.2
 */
public class DirectoryEntryPageProvider extends AbstractPageProvider<DirectoryEntry> {

    @Override
    public List<DirectoryEntry> getCurrentPage() {
        Object[] parameters = getParameters();
        if (parameters == null || parameters.length > 1) {
            throw new IllegalStateException("Invalid parameters: " + Arrays.toString(parameters));
        }

        if (!(parameters[0] instanceof Directory)) {
            throw new IllegalStateException("Provided parameter is not a Directory: " + parameters[0]);
        }

        Directory directory = (Directory) parameters[0];

        try (Session session = directory.getSession()) {
            List<OrderByExpr> orders = getSortInfos().stream().map(this::toOrderExp).collect(Collectors.toList());
            DocumentModelList result = session.query(new QueryBuilder().orders(orders)
                                                                       .limit(getPageSize())
                                                                       .offset(getCurrentPageOffset())
                                                                       .countTotal(true),
                    false);
            setResultsCount(result.totalSize());
            return result.stream()
                         .map(dir -> new DirectoryEntry(directory.getName(), dir))
                         .collect(Collectors.toList());
        }
    }

    protected OrderByExpr toOrderExp(SortInfo sortInfo) {
        String sortColumn = sortInfo.getSortColumn();
        return sortInfo.getSortAscending() ? OrderByExprs.asc(sortColumn) : OrderByExprs.desc(sortColumn);
    }
}
