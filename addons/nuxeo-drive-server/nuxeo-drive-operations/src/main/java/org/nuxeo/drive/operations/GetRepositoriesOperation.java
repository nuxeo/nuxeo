/**
 *
 */

package org.nuxeo.drive.operations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;

/**
 * Fetch the list of the repositories registered on the server. TODO: move this to the list of default operations in
 * 5.7.
 */
@Operation(id = GetRepositoriesOperation.ID, category = Constants.CAT_FETCH, label = "List repository names on the server")
public class GetRepositoriesOperation {

    public static final String ID = "GetRepositories";

    @Context
    protected RepositoryManager repositoryManager;

    @OperationMethod
    public List<String> run() throws ClientException {
        List<String> repositoryNames = new ArrayList<String>(repositoryManager.getRepositoryNames());
        // Make order deterministic to make it simpler to write tests.
        Collections.sort(repositoryNames);
        return repositoryNames;
    }

}
