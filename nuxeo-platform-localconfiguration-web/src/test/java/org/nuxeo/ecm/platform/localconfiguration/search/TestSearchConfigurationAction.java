package org.nuxeo.ecm.platform.localconfiguration.search;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.nuxeo.ecm.webapp.localconfiguration.search.SearchLocalConfiguration;

public class TestSearchConfigurationAction {

    SearchConfigurationActions searchConfigurationActions = new SearchConfigurationActions();

    @Test
    public void testConditionToReturnAChosenAdvancedSearch() {
        SearchLocalConfiguration configuration = null;
        assertFalse(
                "The condition should be False when the SearchLocalConfiguration is null",
                searchConfigurationActions.isLocalConfigurationExistsAndSearchViewAvailable(configuration));
        configuration = mock(SearchLocalConfiguration.class);
        when(configuration.getAdvancedSearchView()).thenReturn(null);
        assertFalse(
                "The condition should be False when the SearchLocalConfiguration is not null and an AdvancedSearchView is null",
                searchConfigurationActions.isLocalConfigurationExistsAndSearchViewAvailable(configuration));
        when(configuration.getAdvancedSearchView()).thenReturn(
                "Choosen Advanced Search View");
        assertTrue(
                "The condition should be True when the SearchLocalConfiguration exists and an AdvancedSearchView exists",
                searchConfigurationActions.isLocalConfigurationExistsAndSearchViewAvailable(configuration));
        configuration = null;
        assertFalse(
                "The condition should be False when the SearchLocalConfiguration is null and an AdvancedSearchView exists",
                searchConfigurationActions.isLocalConfigurationExistsAndSearchViewAvailable(configuration));
    }

}
