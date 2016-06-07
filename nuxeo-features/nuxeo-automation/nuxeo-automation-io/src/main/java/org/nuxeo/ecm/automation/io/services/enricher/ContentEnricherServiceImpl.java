/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.io.services.enricher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.core.HttpHeaders;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ELActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 5.7.3
 * @deprecated The JSON marshalling was migrated to nuxeo-core-io. An enricher system is also available. See
 *             org.nuxeo.ecm.core.io.marshallers.json.enrichers.BreadcrumbJsonEnricher for an example. To migrate an
 *             existing enricher, keep the marshalling code and use it in class implementing
 *             AbstractJsonEnricher&lt;DocumentModel&gt; (the use of contextual parameters is a bit different but
 *             compatible / you have to manage the enricher's parameters yourself). Don't forget to contribute to
 *             service org.nuxeo.ecm.core.io.registry.MarshallerRegistry to register your enricher.
 */
@Deprecated
public class ContentEnricherServiceImpl extends DefaultComponent implements ContentEnricherService {

    /**
    *
    */
    public static final String NXCONTENT_CATEGORY_HEADER = "X-NXContext-Category";

    protected static final Log log = LogFactory.getLog(ContentEnricherServiceImpl.class);

    public static final String ENRICHER = "enricher";

    public static List<Class<?>> DEPRECATED_KNOWN_ENRICHERS = Arrays.asList(ACLContentEnricher.class,
            PreviewContentEnricher.class, UserPermissionsContentEnricher.class);

    private Map<String, ContentEnricherDescriptor> descriptorRegistry = new ConcurrentHashMap<>();

    private Set<Class<?>> customEnrichers = Collections.synchronizedSet(new HashSet<>());

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        List<String> customDeprecated = new ArrayList<>();
        List<Class<?>> deprecatedKnown = Arrays.asList(ACLContentEnricher.class, PreviewContentEnricher.class,
                UserPermissionsContentEnricher.class);
        for (ContentEnricherDescriptor descriptor : descriptorRegistry.values()) {
            if (!deprecatedKnown.contains(descriptor.klass)) {
                customDeprecated.add(descriptor.klass.getSimpleName());
            }
        }
        if (!customDeprecated.isEmpty()) {
            log.warn("The ContentEnricherService is deprecated since 7.10. The contributed enrichers are still available through the deprecated JAX-RS document's marshallers but won't be available through the REST API or automation. You should migrate the following classes and implement "
                    + AbstractJsonEnricher.class.getName() + ": " + StringUtils.join(customDeprecated, ','));
        }
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {

        if (ENRICHER.equals(extensionPoint)) {
            ContentEnricherDescriptor cd = (ContentEnricherDescriptor) contribution;
            if (!DEPRECATED_KNOWN_ENRICHERS.contains(cd.klass)) {
                if (customEnrichers.isEmpty()) {
                    log.warn("The ContentEnricherService is deprecated since 7.10. The contributed enrichers are still available for use through the deprecated JAX-RS document's marshallers but won't be available through the default Nuxeo REST API or Nuxeo Automation. You should migrate your enrichers and implement "
                            + AbstractJsonEnricher.class.getName());
                }
                if (!customEnrichers.contains(cd.klass)) {
                    customEnrichers.add(cd.klass);
                    log.warn("Enrichers registered and not available for use in Nuxeo Rest API and Nuxeo Automation: "
                            + cd.klass.getName());
                }
            }
            descriptorRegistry.put(cd.name, cd);
        }

    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (ENRICHER.equals(extensionPoint)) {
            ContentEnricherDescriptor cd = (ContentEnricherDescriptor) contribution;
            if (descriptorRegistry.containsKey(cd.name)) {
                descriptorRegistry.remove(cd.name);
                if (!DEPRECATED_KNOWN_ENRICHERS.contains(cd.klass)) {
                    customEnrichers.remove(cd.name);
                }
            }
        }
    }

    @Override
    public List<ContentEnricher> getEnrichers(String category, RestEvaluationContext context) {
        List<ContentEnricher> result = new ArrayList<>();
        for (ContentEnricherDescriptor descriptor : getEnricherDescriptors(category, context)) {

            ContentEnricher contentEnricher = descriptor.getContentEnricher();
            result.add(contentEnricher);
        }

        return result;
    }

    private List<ContentEnricherDescriptor> getEnricherDescriptors(String category, RestEvaluationContext context) {
        List<ContentEnricherDescriptor> result = new ArrayList<>();
        for (Entry<String, ContentEnricherDescriptor> entry : descriptorRegistry.entrySet()) {
            ContentEnricherDescriptor descriptor = entry.getValue();
            if (descriptor.categories.contains(category)) {
                result.add(descriptor);
            }
        }
        return result;
    }

    @Override
    public void writeContext(JsonGenerator jg, RestEvaluationContext ec) throws JsonGenerationException, IOException {

        for (String category : getCategoriesToActivate(ec)) {
            for (ContentEnricherDescriptor descriptor : getEnricherDescriptors(category, ec)) {
                if (evaluateFilter(ec, descriptor)) {
                    ContentEnricher enricher = descriptor.getContentEnricher();
                    if (enricher != null) {
                        jg.writeFieldName(descriptor.name);
                        enricher.enrich(jg, ec);
                    }
                }
            }
        }

    }

    /**
     * @param ec
     * @param descriptor
     * @return
     */
    private boolean evaluateFilter(RestEvaluationContext ec, ContentEnricherDescriptor descriptor) {
        for (String filterId : descriptor.filterIds) {
            ActionManager as = Framework.getLocalService(ActionManager.class);
            if (!as.checkFilter(filterId, createActionContext(ec))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates an ActionService compatible ActionContext to evaluate filters
     *
     * @param ec
     * @return
     */
    private ActionContext createActionContext(RestEvaluationContext ec) {
        ActionContext actionContext = new ELActionContext();
        CoreSession session = ec.getDocumentModel().getCoreSession();
        actionContext.setDocumentManager(session);
        actionContext.setCurrentPrincipal((NuxeoPrincipal) session.getPrincipal());

        actionContext.setCurrentDocument(ec.getDocumentModel());

        return actionContext;
    }

    private List<String> getCategoriesToActivate(RestEvaluationContext ec) {
        HttpHeaders headers = ec.getHeaders();
        if (headers != null) {
            List<String> requestHeader = headers.getRequestHeader(NXCONTENT_CATEGORY_HEADER);
            if (requestHeader != null && !requestHeader.isEmpty()) {
                return Arrays.asList(StringUtils.split(requestHeader.get(0), ',', true));
            } else {
                return new ArrayList<>(0);
            }
        }
        return new ArrayList<>(0);
    }

}
