package org.nuxeo.ecm.platform.rendition.lazy;

import org.nuxeo.ecm.platform.rendition.lazy.AutomationLazyRenditionProvider;

/**
 * @since 7.10
 */
public class AutomationLazyPerUserRenditionProvider extends AutomationLazyRenditionProvider {

    @Override
    protected boolean perUserRendition() {
        return true;
    }
}
