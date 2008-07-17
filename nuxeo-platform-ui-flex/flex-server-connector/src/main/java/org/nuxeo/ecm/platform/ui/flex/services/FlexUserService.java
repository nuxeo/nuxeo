package org.nuxeo.ecm.platform.ui.flex.services;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;

@Name("flexUserService")
@Scope(ScopeType.STATELESS)
public class FlexUserService {

    @In(create = true)
    protected transient UserManager userManager;


    protected Map<String,Serializable> mapPrincipal(NuxeoPrincipal principal)
    {
        Map<String,Serializable> flexUser = new HashMap<String, Serializable>();
        flexUser.put("name", principal.getName());
        flexUser.put("firstName", principal.getFirstName());
        flexUser.put("lastName", principal.getLastName());
        flexUser.put("company", principal.getCompany());
        flexUser.put("company", principal.getCompany());
        flexUser.put("label", principal.getName() + " ("+ principal.getFirstName() + " " + principal.getLastName() + ")");
        flexUser.put("data", principal.getName());
        flexUser.put("groups",(Serializable) principal.getAllGroups());

        return flexUser;
    }

    protected Map<String,Serializable> mapGroup(NuxeoGroup group)
    {
        Map<String,Serializable> flexGroup = new HashMap<String, Serializable>();
        flexGroup.put("name", group.getName());
        flexGroup.put("subgroups", (Serializable) group.getMemberGroups());
        flexGroup.put("members", (Serializable) group.getMemberUsers());
        flexGroup.put("parents", (Serializable) group.getParentGroups());
        flexGroup.put("label", group.getName());
        flexGroup.put("data", group.getName());
        return flexGroup;
    }

    @WebRemote
    public List<Object> getUsers(String userNamePattern) throws ClientException
    {
        List<NuxeoPrincipal> users= userManager.searchPrincipals(userNamePattern);

        List<Object> flexUsers = new ArrayList<Object>();

        for (NuxeoPrincipal nxUser : users)
        {
            flexUsers.add((Serializable)mapPrincipal(nxUser));
        }

        return flexUsers;
    }

    @WebRemote
    public Object getUser(String userName) throws ClientException
    {
        NuxeoPrincipal user = userManager.getPrincipal(userName);

        if (user==null)
            return null;

        return mapPrincipal(user);
    }

    @WebRemote
    public Object updateUser(String userName, Object userValues) throws ClientException
    {
        NuxeoPrincipal user = userManager.getPrincipal(userName);

        Map<String,Serializable> userMap = (Map<String,Serializable>) userValues;

        if (userMap.get("firstName")!=null)
            user.setFirstName((String)userMap.get("firstName"));
        if (userMap.get("lastName")!=null)
            user.setLastName((String)userMap.get("lastName"));
        if (userMap.get("company")!=null)
            user.setCompany((String)userMap.get("company"));
        if (userMap.get("password")!=null)
            user.setPassword((String)userMap.get("password"));

        if (userMap.get("groups")!=null)
        {
            List<String> groups = (List<String>)userMap.get("groups");
            for(String newGroup : groups)
            {
                if (!user.getAllGroups().contains(newGroup))
                {
                    List<String> userGroups = user.getGroups();
                    userGroups.add(newGroup);
                    user.setGroups(userGroups);
                }
            }
        }
        userManager.updatePrincipal(user);
        return mapPrincipal(userManager.getPrincipal(userName));
    }


    @WebRemote
    public Object createUser(Object userValues) throws ClientException
    {
        Map<String,Serializable> userMap = (Map<String,Serializable>) userValues;

        String userName=null;
        if (userMap.get("name")!=null)
            userName=(String) userMap.get("name");
        else
            throw new ClientException("User must have a login name");

        NuxeoPrincipal user = userManager.getPrincipal(userName);
        if (user!=null)
            throw new ClientException("User alreaedy exists");

        user = new NuxeoPrincipalImpl(userName);
        if (userMap.get("firstName")!=null)
            user.setFirstName((String)userMap.get("firstName"));
        if (userMap.get("lastName")!=null)
            user.setLastName((String)userMap.get("lastName"));
        if (userMap.get("company")!=null)
            user.setCompany((String)userMap.get("company"));

        if (userMap.get("password")!=null)
            user.setPassword((String)userMap.get("password"));

        if (userMap.get("groups")!=null)
        {
            List<String> groups = (List<String>)userMap.get("groups");
            user.setGroups(groups);
        }

        userManager.createPrincipal(user);
        return mapPrincipal(userManager.getPrincipal(userName));
    }

    @WebRemote
    public List<Object> getGroups(String groupNamePattern) throws ClientException
    {
        List<NuxeoGroup> groups= userManager.searchGroups(groupNamePattern);

        List<Object> flexGroups = new ArrayList<Object>();

        for (NuxeoGroup nxGroup : groups)
        {
            flexGroups.add((Serializable)mapGroup(nxGroup));
        }

        return flexGroups;
    }


    @WebRemote
    public List<String> getUserNames(String userNamePattern) throws ClientException
    {
        List<NuxeoPrincipal> users= userManager.searchPrincipals(userNamePattern);

        List<String> flexUserNames = new ArrayList<String>();

        for (NuxeoPrincipal nxUser : users)
        {
            flexUserNames.add(nxUser.getName());
        }

        return flexUserNames;
    }

    @WebRemote
    public List<String> getGroupNames(String groupNamePattern) throws ClientException
    {
        List<NuxeoGroup> groups= userManager.searchGroups(groupNamePattern);

        List<String> flexGroupNamess = new ArrayList<String>();

        for (NuxeoGroup nxGroup : groups)
        {
            flexGroupNamess.add(nxGroup.getName());
        }

        return flexGroupNamess;
    }

    @WebRemote
    public Map<String,Serializable> getGroup(String groupName) throws ClientException
    {
        NuxeoGroup group = userManager.getGroup(groupName);

        if (group==null)
            return null;

        return mapGroup(group);
    }


    @WebRemote
    public void addUserToGroup(String userName, String groupName) throws ClientException
    {
        NuxeoPrincipal user = userManager.getPrincipal(userName);
        NuxeoGroup group = userManager.getGroup(groupName);

        if ((group==null) || (user==null))
            return;

        group.getMemberUsers().add(userName);
        userManager.updateGroup(group);
        //user.getGroups().add(group.getName());
        //userManager.updatePrincipal(user);
    }

}
