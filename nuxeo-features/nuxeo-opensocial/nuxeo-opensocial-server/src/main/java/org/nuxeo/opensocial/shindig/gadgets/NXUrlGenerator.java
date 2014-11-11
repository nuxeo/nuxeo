package org.nuxeo.opensocial.shindig.gadgets;

import java.util.Collection;

import org.apache.shindig.common.ContainerConfig;
import org.apache.shindig.gadgets.DefaultUrlGenerator;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetFeatureRegistry;
import org.apache.shindig.gadgets.LockedDomainService;
import org.nuxeo.runtime.api.Framework;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/** @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a> */
@Singleton
public class NXUrlGenerator extends DefaultUrlGenerator {

    @Inject
    public NXUrlGenerator(ContainerConfig containerConfig,
            LockedDomainService lockedDomainService,
            GadgetFeatureRegistry registry) {
        super(containerConfig, lockedDomainService, registry);
    }

    @Override
    public String getBundledJsUrl(Collection<String> features,
            GadgetContext context) {
        String url = super.getBundledJsUrl(features, context);
        return url.replace("%contextPath%", Framework.getProperty(
                "org.nuxeo.ecm.contextPath", "/nuxeo"));
    }
}
