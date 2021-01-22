/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.htmlsanitizer;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

/**
 * Service that sanitizes some HMTL fields to remove potential cross-site scripting attacks in them.
 */
public class HtmlSanitizerServiceImpl extends DefaultComponent implements HtmlSanitizerService {

    private static final Logger log = LogManager.getLogger(HtmlSanitizerServiceImpl.class);

    public static final String ANTISAMY_XP = "antisamy";

    public static final String SANITIZER_XP = "sanitizer";

    /** Effective policy. */
    protected PolicyFactory policy;

    /** Effective sanitizers. */
    protected List<HtmlSanitizerDescriptor> sanitizers;

    @Override
    public void start(ComponentContext context) {
        this.<HtmlSanitizerAntiSamyDescriptor> getRegistryContribution(ANTISAMY_XP).ifPresent(desc -> {
            URL url = desc.policy.toURL();
            HtmlPolicyBuilder builder = new HtmlPolicyBuilder();
            try {
                builder.loadAntiSamyPolicy(url);
                initializeBuilder(builder);
                policy = builder.toFactory();
            } catch (IOException e) {
                policy = null;
                throw new NuxeoException("Cannot parse AntiSamy policy: " + desc.policy, e);
            }
        });
        sanitizers = new ArrayList<>(1);
        this.<HtmlSanitizerDescriptor> getRegistryContributions(SANITIZER_XP).forEach(desc -> {
            if (desc.fields.isEmpty()) {
                log.error("Sanitizer has no fields: {}", desc);
                return;
            }
            sanitizers.add(desc);
        });

    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        policy = null;
        sanitizers = null;
    }

    protected void initializeBuilder(HtmlPolicyBuilder builder) {
        builder.allowStandardUrlProtocols();
        builder.allowUrlProtocols("data"); // still enforces regex matchers from policy
        builder.allowStyling();
        builder.disallowElements("script");
    }

    protected List<HtmlSanitizerDescriptor> getSanitizers() {
        return sanitizers;
    }

    // ----- HtmlSanitizerService -----

    @Override
    public void sanitizeDocument(DocumentModel doc) {
        if (policy == null) {
            log.error("Cannot sanitize, no policy registered");
            return;
        }
        for (HtmlSanitizerDescriptor sanitizer : sanitizers) {
            if (!sanitizer.types.isEmpty() && !sanitizer.types.contains(doc.getType())) {
                continue;
            }
            for (FieldDescriptor field : sanitizer.fields) {
                String fieldName = field.getContentField();
                String filterField = field.getFilterField();
                if (filterField != null) {
                    Property filterProp;
                    try {
                        filterProp = doc.getProperty(filterField);
                    } catch (PropertyNotFoundException e) {
                        continue;
                    }
                    if (field.match(String.valueOf(filterProp.getValue())) != field.doSanitize()) {
                        continue;
                    }
                }
                Property prop;
                try {
                    prop = doc.getProperty(fieldName);
                } catch (PropertyNotFoundException e) {
                    continue;
                }
                Serializable value = prop.getValue();
                if (value == null) {
                    continue;
                }
                if (!(value instanceof String)) {
                    log.debug("Cannot sanitize non-string field: {}", field);
                    continue;
                }
                String info = "doc " + doc.getPathAsString() + " (" + doc.getId() + ") field " + field;
                String newValue = sanitizeString((String) value, info);
                if (!newValue.equals(value)) {
                    prop.setValue(newValue);
                }
            }
        }
    }

    @Override
    public String sanitizeString(String string, String info) {
        if (policy == null) {
            log.error("Cannot sanitize, no policy registered");
            return string;
        }
        return policy.sanitize(string);
    }

}
