package org.nuxeo.ecm.platform.audit.service;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.service.extension.ExtendedInfoDescriptor;
import org.nuxeo.ecm.platform.el.ExpressionEvaluator;

public class ExtendedInfoInjector  {

    public ExtendedInfoInjector(ExpressionEvaluator evaluator) {
        super();
        this.evaluator = evaluator;
    }
    
    protected final ExpressionEvaluator evaluator;

    public void injectExtendedInfo(Map<String, ExtendedInfo> infos,
            ExtendedInfoDescriptor descriptor,
            ExtendedInfoContext context) {
        Serializable value = (Serializable) evaluator.evaluateExpression(
                context, descriptor.getExpression(), Serializable.class);
        if (value == null)
            return;
        String key = descriptor.getKey();
        infos.put(key, ExtendedInfo.createExtendedInfo(value));
    }

}
