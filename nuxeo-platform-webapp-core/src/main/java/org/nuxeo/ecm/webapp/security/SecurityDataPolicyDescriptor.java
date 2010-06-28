package org.nuxeo.ecm.webapp.security;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.webapp.security.policies.DefaultSecurityDataPolicy;

@XObject(value="policy")
public class SecurityDataPolicyDescriptor {


    @XNode("@class")
    public Class<? extends SecurityDataPolicy> clazz = DefaultSecurityDataPolicy.class;

    public SecurityDataPolicy newPolicy() {
        try {
            return clazz.newInstance();
        } catch (Exception e) {
            throw new ClientRuntimeException("Cannot instantiate policy", e);
        }
    }


}
