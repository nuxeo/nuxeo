package org.nuxeo.ecm.platform.template.service;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.template.processors.TemplateProcessor;

public interface TemplateProcessorService {

    String findProcessorName(Blob templateBlob);

    TemplateProcessor findProcessor(Blob templateBlob);

    TemplateProcessor getProcessor(String name);

}
