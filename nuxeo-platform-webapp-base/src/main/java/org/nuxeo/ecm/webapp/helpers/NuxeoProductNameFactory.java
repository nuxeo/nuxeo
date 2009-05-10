package org.nuxeo.ecm.webapp.helpers;

import java.io.Serializable;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.runtime.api.Framework;

import static org.jboss.seam.annotations.Install.FRAMEWORK;

@Name("appNameFactory")
@Scope(ScopeType.STATELESS)
@Install(precedence=FRAMEWORK)
public class NuxeoProductNameFactory implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;


    public static String PNAME_KEY = "org.nuxeo.ecm.product.name";
    public static String PVERSION_KEY = "org.nuxeo.ecm.product.version";

    @Factory(value="nuxeoApplicationName", scope=ScopeType.APPLICATION)
    public String getNuxeoProductName() {
        return Framework.getProperty(PNAME_KEY);
    }


}
