/*
 * (C) Copyright 2013-2018 Nuxeo (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.usermanager.providers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.SizeLimitExceededException;
import org.nuxeo.ecm.core.query.sql.model.MultiExpression;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.OrderByExpr;
import org.nuxeo.ecm.core.query.sql.model.OrderByExprs;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.platform.query.api.AbstractPageProvider;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import java.util.stream.Collectors;

/**
 * Abstract Page provider listing groups.
 * <p>
 * This page provider requires one parameter: the first one to be filled with the search string.
 * <p>
 * This page provider requires the property {@link #GROUPS_LISTING_MODE_PROPERTY} to be filled with a the listing mode
 * to use.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.8
 */
public abstract class AbstractGroupsPageProvider<T> extends AbstractPageProvider<T> {

    private static final Logger log = LogManager.getLogger(AbstractGroupsPageProvider.class);

    protected static final String GROUPS_LISTING_MODE_PROPERTY = "groupsListingMode";

    protected static final String ALL_MODE = "all";

    protected static final String SEARCH_ONLY_MODE = "search_only";

    protected static final String SEARCH_OVERFLOW_ERROR_MESSAGE = "label.security.searchOverFlow";

    private static final long serialVersionUID = 1L;

    protected List<DocumentModel> pageGroups;

    public List<DocumentModel> computeCurrentPage() {
        if (pageGroups == null) {
            error = null;
            errorMessage = null;
            pageGroups = new ArrayList<>();

            try {
                UserManager userManager = Framework.getService(UserManager.class);
                String groupListingMode = getGroupListingMode();
                String searchString = getFirstParameter();
                if (ALL_MODE.equals(groupListingMode) || "*".equals(searchString)) {
                    searchString = null;
                } else if (StringUtils.isEmpty(searchString)) {
                    setResultsCount(0);
                    return pageGroups;
                }
                long pageSize = getMinMaxPageSize();
                long offset = getCurrentPageOffset();
                OrderByExpr order = OrderByExprs.asc(
                        StringUtils.defaultString(userManager.getGroupSortField(), userManager.getGroupIdField()));
                QueryBuilder qb = new QueryBuilder();
                if (StringUtils.isNotBlank(searchString)) {
                    String pattern = searchString.trim().toLowerCase() + '%';
                    List<Predicate> predicates = userManager.getGroupSearchFields()
                                                             .stream()
                                                             .map(f -> Predicates.ilike(f, pattern))
                                                             .collect(Collectors.toList());
                    qb.filter(new MultiExpression(Operator.OR, predicates));
                }
                qb.order(order).countTotal(true);
                if (pageSize > 0) {
                    qb.limit(pageSize);
                }
                if (offset > 0) {
                    qb.offset(offset);
                }
                DocumentModelList groups = userManager.searchGroups(qb);
                setResultsCount(groups.totalSize());
                pageGroups.addAll(groups);
            } catch (SizeLimitExceededException slee) {
                error = slee;
                errorMessage = SEARCH_OVERFLOW_ERROR_MESSAGE;
                log.warn(slee.getMessage(), slee);
            }
        }
        return pageGroups;
    }

    protected List<DocumentModel> searchAllGroups() {
        return Framework.getService(UserManager.class).searchGroups(Collections.emptyMap(), null);
    }

    protected List<DocumentModel> searchGroups() {
        UserManager userManager = Framework.getService(UserManager.class);
        List<DocumentModel> groups = new ArrayList<>();
        String searchString = getFirstParameter();
        if ("*".equals(searchString)) {
            groups = searchAllGroups();
        } else if (!StringUtils.isEmpty(searchString)) {
            groups = userManager.searchGroups(searchString);
        }
        return groups;
    }

    protected String getGroupListingMode() {
        Map<String, Serializable> props = getProperties();
        if (props.containsKey(GROUPS_LISTING_MODE_PROPERTY)) {
            return (String) props.get(GROUPS_LISTING_MODE_PROPERTY);
        }
        return SEARCH_ONLY_MODE;
    }

    protected String getFirstParameter() {
        Object[] parameters = getParameters();
        if (parameters.length > 0) {
            String param = (String) parameters[0];
            if (param != null) {
                return param.trim();
            }
        }
        return "";
    }

    /**
     * This page provider does not support sort for now =&gt; override what may be contributed in the definition
     */
    @Override
    public boolean isSortable() {
        return false;
    }

    @Override
    protected void pageChanged() {
        pageGroups = null;
        super.pageChanged();
    }

    @Override
    public void refresh() {
        pageGroups = null;
        super.refresh();
    }

}
