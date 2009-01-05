package org.nuxeo.ecm.core.convert.extension;

import org.nuxeo.ecm.core.convert.api.ConversionService;

public interface ConversionServiceManager extends ConversionService {

    Converter getConverter(String converterName);


}
