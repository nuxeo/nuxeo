package org.nuxeo.ecm.core.api.security;

import java.io.Serializable;

public class UserVisiblePermission implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    protected String permission;

    protected String denyPermission;

    protected String id;

    public UserVisiblePermission(String id, String perm, String denyPerm)
    {
        this.permission=perm;
        this.denyPermission=denyPerm;
        this.id=id;
    }


    @Override
    public boolean equals(Object other) {

        if (other==null)
            return false;

        if (this.toString().equals(other.toString()))
            return true;
        else
            return false;
    }


    @Override
    public String toString() {
        if (denyPermission!=null)
            return String.format("UserVisiblePermission %s [%s (deny %s)]", id,  permission, denyPermission);
        else
            return String.format("UserVisiblePermission %s [%s]",id,  permission);
    }


    public String getPermission() {
        return permission;
    }


    public void setPermission(String permission) {
        this.permission = permission;
    }


    public String getDenyPermission() {
        return denyPermission;
    }


    public void setDenyPermission(String denyPermission) {
        this.denyPermission = denyPermission;
    }


    public String getId() {
        return id;
    }


    public void setId(String id) {
        this.id = id;
    }


}
