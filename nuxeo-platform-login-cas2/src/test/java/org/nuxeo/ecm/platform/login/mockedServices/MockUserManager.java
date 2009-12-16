package org.nuxeo.ecm.platform.login.mockedServices;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.UserManagerDescriptor;
import org.nuxeo.ecm.platform.usermanager.exceptions.GroupAlreadyExistsException;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;

public class MockUserManager implements UserManager{

    private static final long serialVersionUID = 1L;

    public Boolean areGroupsReadOnly() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public Boolean areUsersReadOnly() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean checkUsernamePassword(String username, String password)
            throws ClientException {
        // TODO Auto-generated method stub
        return false;
    }

    public DocumentModel createGroup(DocumentModel groupModel)
            throws ClientException, GroupAlreadyExistsException {
        // TODO Auto-generated method stub
        return null;
    }

    public void createGroup(NuxeoGroup group) throws ClientException {
        // TODO Auto-generated method stub
        
    }

    public void createPrincipal(NuxeoPrincipal principal)
            throws ClientException {
        // TODO Auto-generated method stub
        
    }

    public DocumentModel createUser(DocumentModel userModel)
            throws ClientException, UserAlreadyExistsException {
        // TODO Auto-generated method stub
        return null;
    }

    public void deleteGroup(DocumentModel groupModel) throws ClientException {
        // TODO Auto-generated method stub
        
    }

    public void deleteGroup(String groupId) throws ClientException {
        // TODO Auto-generated method stub
        
    }

    public void deleteGroup(NuxeoGroup group) throws ClientException {
        // TODO Auto-generated method stub
        
    }

    public void deletePrincipal(NuxeoPrincipal principal)
            throws ClientException {
        // TODO Auto-generated method stub
        
    }

    public void deleteUser(DocumentModel userModel) throws ClientException {
        // TODO Auto-generated method stub
        
    }

    public void deleteUser(String userId) throws ClientException {
        // TODO Auto-generated method stub
        
    }

    public String getAnonymousUserId() throws ClientException {
        return "Anonymous";
    }

    public List<NuxeoGroup> getAvailableGroups() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public List<NuxeoPrincipal> getAvailablePrincipals() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel getBareGroupModel() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel getBareUserModel() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getDefaultGroup() {
        // TODO Auto-generated method stub
        return null;
    }

    public NuxeoGroup getGroup(String groupName) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getGroupDirectoryName() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getGroupIdField() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public List<String> getGroupIds() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getGroupListingMode() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getGroupMembersField() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel getGroupModel(String groupName) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getGroupParentGroupsField() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getGroupSchemaName() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getGroupSubGroupsField() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public List<String> getGroupsInGroup(String parentId)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel getModelForUser(String name) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public NuxeoPrincipal getPrincipal(String username) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public List<String> getTopLevelGroups() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getUserDirectoryName() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getUserEmailField() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getUserIdField() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public List<String> getUserIds() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getUserListingMode() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel getUserModel(String userName) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public Pattern getUserPasswordPattern() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getUserSchemaName() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public Set<String> getUserSearchFields() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getUserSortField() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public List<String> getUsersInGroup(String groupId) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public List<String> getUsersInGroupAndSubGroups(String groupId)
            throws ClientException {
        return null;
    }
    
    public List<NuxeoPrincipal> searchByMap(Map<String, Serializable> filter,
            Set<String> pattern) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public List<NuxeoGroup> searchGroups(String pattern) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelList searchGroups(Map<String, Serializable> filter,
            HashSet<String> fulltext) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public List<NuxeoPrincipal> searchPrincipals(String pattern)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelList searchUsers(String pattern) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelList searchUsers(Map<String, Serializable> filter,
            Set<String> fulltext) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public void setConfiguration(UserManagerDescriptor descriptor)
            throws ClientException {
        // TODO Auto-generated method stub
        
    }

    public void updateGroup(DocumentModel groupModel) throws ClientException {
        // TODO Auto-generated method stub
        
    }

    public void updateGroup(NuxeoGroup group) throws ClientException {
        // TODO Auto-generated method stub
        
    }

    public void updatePrincipal(NuxeoPrincipal principal)
            throws ClientException {
        // TODO Auto-generated method stub
        
    }

    public void updateUser(DocumentModel userModel) throws ClientException {
        // TODO Auto-generated method stub
        
    }

    public boolean validatePassword(String password) throws ClientException {
        // TODO Auto-generated method stub
        return false;
    }

}
