package org.nuxeo.ecm.core.convert.extension;

import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;

/**
 * Interface that must be implemented by Converter that depend on an external
 * service.
 * <p>
 * Compared to {@link Converter} interface, it adds support for
 * checking converter availability.
 *
 * @author tiry
 */
public interface ExternalConverter extends Converter {

    /**
     * Check if the converter is available.
     *
     * @return
     */
    ConverterCheckResult isConverterAvailable();

}
