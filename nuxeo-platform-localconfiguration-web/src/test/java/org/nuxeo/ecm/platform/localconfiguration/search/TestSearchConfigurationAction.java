package org.nuxeo.ecm.platform.localconfiguration.search;

import static org.junit.Assert.*;

import org.junit.Test;
import org.nuxeo.ecm.webapp.localconfiguration.search.SearchLocalConfiguration;
import static org.mockito.Mockito.*;

public class TestSearchConfigurationAction {

	SearchConfigurationActions searchConfigurationActions = new SearchConfigurationActions();

	@Test
	public void testConditionToReturnAChosenAdvancedSearch() {
		SearchLocalConfiguration configuration = null;
		assertFalse(
				"The condition should be False when the SearchLoaclConfiguration is null",
				searchConfigurationActions
						.isLocalConfigurationExistsAndSearchViewAvailable(configuration));
		configuration = mock(SearchLocalConfiguration.class);
		when(configuration.getAdvancedSearchView()).thenReturn(null);
		assertFalse(
				"The condition should be False when the SearchLoaclConfiguration is not null and an advancedSearchView is null",
				searchConfigurationActions
						.isLocalConfigurationExistsAndSearchViewAvailable(configuration));
		when(configuration.getAdvancedSearchView()).thenReturn("Valeur par defaut");
		assertTrue(
				"The condition should be True when the SearchLoaclConfiguration exists and an advancedSearchView exists",
				searchConfigurationActions
						.isLocalConfigurationExistsAndSearchViewAvailable(configuration));
		configuration=null;
		assertFalse(
				"The condition should be False when the SearchLoaclConfiguration is null and an advancedSearchView exists",
				searchConfigurationActions
						.isLocalConfigurationExistsAndSearchViewAvailable(configuration));
	}

}
