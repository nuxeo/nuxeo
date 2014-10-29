package org.nuxeo.ecm.directory;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.directory.Session.Right;

/**
 * Signal unauthorized access to the directory session.
 *
 * @author Stephane Lacoin at Nuxeo (aka matic)
 *
 * @since 5.9.6
 *
 */
public class DirectorySecurityException extends DirectoryException {

    private static final long serialVersionUID = 1L;

    public final Right right;

    public final NuxeoPrincipal owner;

    public DirectorySecurityException(SecuredSession session, Right right) {
        super(
                String
                    .format("User '%s', is not allowed for permission '%s' on the directory '%s'",
                            session.owner.getName(), right.name(), session.directory.getName()));
        this.right = right;
        owner = session.owner;
    }

}
