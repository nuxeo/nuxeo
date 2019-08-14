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

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.owasp.validator.html.AntiSamy;
import org.owasp.validator.html.CleanResults;
import org.owasp.validator.html.Policy;
import org.owasp.validator.html.PolicyException;
import org.owasp.validator.html.ScanException;

/**
 * Service that sanitizes some HMTL fields to remove potential cross-site scripting attacks in them.
 */
public class HtmlSanitizerServiceImpl extends DefaultComponent implements HtmlSanitizerService {

    private static final Log log = LogFactory.getLog(HtmlSanitizerServiceImpl.class);

    public static final String ANTISAMY_XP = "antisamy";

    public static final String SANITIZER_XP = "sanitizer";

    /** All policies registered. */
    public LinkedList<HtmlSanitizerAntiSamyDescriptor> allPolicies = new LinkedList<HtmlSanitizerAntiSamyDescriptor>();

    /** Effective policy. */
    public Policy policy;

    /** All sanitizers registered. */
    public List<HtmlSanitizerDescriptor> allSanitizers = new ArrayList<HtmlSanitizerDescriptor>(1);

    /** Effective sanitizers. */
    public List<HtmlSanitizerDescriptor> sanitizers = new ArrayList<HtmlSanitizerDescriptor>(1);

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (ANTISAMY_XP.equals(extensionPoint)) {
            if (!(contribution instanceof HtmlSanitizerAntiSamyDescriptor)) {
                log.error("Contribution " + contribution + " is not of type "
                        + HtmlSanitizerAntiSamyDescriptor.class.getName());
                return;
            }
            HtmlSanitizerAntiSamyDescriptor desc = (HtmlSanitizerAntiSamyDescriptor) contribution;
            log.info("Registering AntiSamy policy: " + desc.policy);
            addAntiSamy(desc);
        } else if (SANITIZER_XP.equals(extensionPoint)) {
            if (!(contribution instanceof HtmlSanitizerDescriptor)) {
                log.error("Contribution " + contribution + " is not of type " + HtmlSanitizerDescriptor.class.getName());
                return;
            }
            HtmlSanitizerDescriptor desc = (HtmlSanitizerDescriptor) contribution;
            log.info("Registering HTML sanitizer: " + desc);
            addSanitizer(desc);
        } else {
            log.error("Contribution extension point should be '" + SANITIZER_XP + "' but is: " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (ANTISAMY_XP.equals(extensionPoint)) {
            if (!(contribution instanceof HtmlSanitizerAntiSamyDescriptor)) {
                return;
            }
            HtmlSanitizerAntiSamyDescriptor desc = (HtmlSanitizerAntiSamyDescriptor) contribution;
            log.info("Unregistering AntiSamy policy: " + desc.policy);
            removeAntiSamy(desc);
        } else if (SANITIZER_XP.equals(extensionPoint)) {
            if (!(contribution instanceof HtmlSanitizerDescriptor)) {
                return;
            }
            HtmlSanitizerDescriptor desc = (HtmlSanitizerDescriptor) contribution;
            log.info("Unregistering HTML sanitizer: " + desc);
            removeSanitizer(desc);
        }
    }

    protected void addAntiSamy(HtmlSanitizerAntiSamyDescriptor desc) {
        if (Thread.currentThread().getContextClassLoader().getResourceAsStream(desc.policy) == null) {
            log.error("Cannot find AntiSamy policy: " + desc.policy);
            return;
        }
        allPolicies.add(desc);
        refreshPolicy();
    }

    protected void removeAntiSamy(HtmlSanitizerAntiSamyDescriptor desc) {
        allPolicies.remove(desc);
        refreshPolicy();
    }

    protected void refreshPolicy() {
        if (allPolicies.isEmpty()) {
            policy = null;
        } else {
            HtmlSanitizerAntiSamyDescriptor desc = allPolicies.removeLast();
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(desc.policy);
            try {
                policy = Policy.getInstance(is);
            } catch (PolicyException e) {
                policy = null;
                throw new RuntimeException("Cannot parse AntiSamy policy: " + desc.policy, e);
            }
        }
    }

    protected Policy getPolicy() {
        return policy;
    }

    protected void addSanitizer(HtmlSanitizerDescriptor desc) {
        if (desc.fields.isEmpty()) {
            log.error("Sanitizer has no fields: " + desc);
            return;
        }
        allSanitizers.add(desc);
        refreshSanitizers();
    }

    protected void removeSanitizer(HtmlSanitizerDescriptor desc) {
        allSanitizers.remove(desc);
        refreshSanitizers();
    }

    protected void refreshSanitizers() {
        // not very efficient algorithm but who cares?
        sanitizers.clear();
        for (HtmlSanitizerDescriptor sanitizer : allSanitizers) {
            // remove existing with same name
            for (Iterator<HtmlSanitizerDescriptor> it = sanitizers.iterator(); it.hasNext();) {
                HtmlSanitizerDescriptor s = it.next();
                if (s.name.equals(sanitizer.name)) {
                    it.remove();
                    break;
                }
            }
            // add new one if enabled
            if (sanitizer.enabled) {
                sanitizers.add(sanitizer);
            }
        }
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
                    log.debug("Cannot sanitize non-string field: " + field);
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
        try {
            CleanResults cr = new AntiSamy().scan(string, policy);
            for (Object err : cr.getErrorMessages()) {
                log.debug(String.format("Sanitizing %s: %s", info == null ? "" : info, err));
            }
            return cr.getCleanHTML();
        } catch (ScanException | PolicyException e) {
            log.error(String.format("Cannot sanitize %s: %s", info == null ? "" : info, e));
            String msg = e.getMessage();
            if (msg.startsWith("The input was too large")) {
                return msg + " Please check with the server administrator to increase the maximum input size.";
            } else {
                return "Error while sanitizing HTML, please check the server logs";
            }
        }
    }

}
