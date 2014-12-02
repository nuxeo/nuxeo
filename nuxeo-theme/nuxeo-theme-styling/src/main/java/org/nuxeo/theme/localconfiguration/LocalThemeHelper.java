package org.nuxeo.theme.localconfiguration;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.localconfiguration.LocalConfigurationService;
import org.nuxeo.runtime.api.Framework;

public class LocalThemeHelper {

    public static LocalThemeConfig getLocalThemeConfig(DocumentModel doc) {
        LocalConfigurationService localConfigurationService = Framework.getService(LocalConfigurationService.class);
        return localConfigurationService.getConfiguration(
                LocalThemeConfig.class,
                LocalThemeConfigConstants.THEME_CONFIGURATION_FACET, doc);
    }

}
