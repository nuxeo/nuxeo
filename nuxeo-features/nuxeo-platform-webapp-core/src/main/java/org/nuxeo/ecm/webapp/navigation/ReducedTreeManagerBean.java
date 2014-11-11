package org.nuxeo.ecm.webapp.navigation;

import static org.jboss.seam.ScopeType.PAGE;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

/**
 * Manages the tree for reduced utilization, like for popup.
 * Performs additional work when a node is selected such as
 * saving the selection and redirecting towards the required page.
 *
 * The scope is PAGE to reinitialize the tree for new utilization.
 *
 * @author <a href="mailto:tmartins@nuxeo.com">Thierry Martins</a>
 */
@Scope(PAGE)
@Name("reducedTreeManager")
@Install(precedence = FRAMEWORK)
public class ReducedTreeManagerBean extends TreeManagerBean {

	private static final long serialVersionUID = 1L;


}
