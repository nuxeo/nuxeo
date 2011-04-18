package org.nuxeo.ecm.platform.wi.backend;

/**
 * Date: 04.03.2011
 * Time: 0:06:58
 *
 * @author Vitalii Siryi
 */
public class SearchBackendFactory extends AbstractBackendFactory {

    @Override
    protected Backend createRootBackend() {
        return new SearchRootBackend();
    }
}
