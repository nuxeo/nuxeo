package org.nuxeo.ecm.platform.ui.web.contentview;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.SortInfo;

public interface PageProviderDescriptor {

    Map<String, String> getProperties();

    void setPattern(String pattern);

    String getDocType();

    String getPattern();

    WhereClauseDescriptor getWhereClause();

    boolean isSortable();

    List<SortInfo> getSortInfos();

    long getPageSize();

}