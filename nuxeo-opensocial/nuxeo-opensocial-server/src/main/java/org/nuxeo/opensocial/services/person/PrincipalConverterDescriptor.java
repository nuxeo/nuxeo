package org.nuxeo.opensocial.services.person;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;


@XObject("principalConverter")
public class PrincipalConverterDescriptor {
    @XNode("@class")
    private Class<? extends PrincipalConverter> converterClass;

    public Class<? extends PrincipalConverter> getPrincipalConverterClass() {
        return converterClass;
    }

}
