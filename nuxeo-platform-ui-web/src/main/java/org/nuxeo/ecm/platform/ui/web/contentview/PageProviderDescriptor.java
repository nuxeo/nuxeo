package org.nuxeo.ecm.platform.ui.web.contentview;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.PageProvider;
import org.nuxeo.ecm.core.api.SortInfo;

/**
 * Page provider descriptor interface handling all attributes common to a
 * {@link PageProvider} generation.
 *
 * @author Anahide Tchertchian
 */
public interface PageProviderDescriptor extends Serializable {

    Map<String, String> getProperties();

    String[] getQueryParameters();

    boolean getQuotePatternParameters();

    void setPattern(String pattern);

    String getPattern();

    WhereClauseDescriptor getWhereClause();

    boolean isSortable();

    List<SortInfo> getSortInfos();

    long getPageSize();

}