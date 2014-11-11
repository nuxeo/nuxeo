package org.nuxeo.theme.localconfiguration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.localconfiguration.LocalConfigurationService;
import org.nuxeo.runtime.api.Framework;

public class LocalThemeHelper {

    private static final Log log = LogFactory.getLog(LocalThemeHelper.class);

    public static LocalThemeConfig getLocalThemeConfig(DocumentModel doc) {
        LocalThemeConfig configuration = null;
        try {
            LocalConfigurationService localConfigurationService = Framework.getService(LocalConfigurationService.class);
            configuration = localConfigurationService.getConfiguration(
                    LocalThemeConfig.class, LocalThemeConfigConstants.THEME_CONFIGURATION_FACET,
                    doc);
        } catch (Exception e) {
            log.error(e, e);
        }
        return configuration;
    }

}
