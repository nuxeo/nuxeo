package org.nuxeo.ecm.webapp.context;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;

import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.util.RepositoryLocation;

@Name("serverLocator")
@Scope(CONVERSATION)
@Install(precedence=FRAMEWORK)
/**
 * Externalize serverLocation Factory to avoid NavigationContext reentrant calls
 */
public class ServerContextBean implements Serializable {

	private static final long serialVersionUID = 9768768768761L;

	private RepositoryLocation currentServerLocation;


    @Factory(value = "currentServerLocation", scope = EVENT)
    public RepositoryLocation getCurrentServerLocation() {
        return currentServerLocation;
    }


    public void setRepositoryLocation(RepositoryLocation serverLocation)
    {
    	this.currentServerLocation=serverLocation;
    }

}
