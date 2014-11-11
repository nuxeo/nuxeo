package org.nuxeo.ecm.platform.login;

import java.security.Principal;
import java.security.acl.Group;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import sun.security.acl.GroupImpl;



public abstract class NuxeoAbstractServerLoginModule implements LoginModule {

    protected Subject subject;
    protected Map sharedState;
    protected Map options;

    protected boolean loginOk;
    /** An optional custom Principal class implementation */
    protected String principalClassName;
    /** the principal to use when a null username and password are seen */
    protected Principal unauthenticatedIdentity;

    protected CallbackHandler callbackHandler;

    /** Flag indicating if the shared credential should be used */
    protected boolean useFirstPass;

    private static final Log log = LogFactory.getLog(NuxeoAbstractServerLoginModule.class);


    abstract protected Principal getIdentity();
    abstract protected Group[] getRoleSets() throws LoginException;
    abstract Principal createIdentity(String username) throws Exception;

    public boolean abort() throws LoginException
    {
       log.trace("abort");
       return true;
    }

    public boolean commit() throws LoginException
    {
       log.trace("commit, loginOk="+loginOk);
       if( loginOk == false )
          return false;

       Set principals = subject.getPrincipals();
       Principal identity = getIdentity();
       principals.add(identity);
       Group[] roleSets = getRoleSets();
       for(int g = 0; g < roleSets.length; g ++)
       {
          Group group = roleSets[g];
          String name = group.getName();
          Group subjectGroup = createGroup(name, principals);

          /*if( subjectGroup instanceof NestableGroup )
          {
             SimpleGroup tmp = new SimpleGroup("Roles");
             subjectGroup.addMember(tmp);
             subjectGroup = tmp;
          }*/

          // Copy the group members to the Subject group
          Enumeration members = group.members();
          while( members.hasMoreElements() )
          {
             Principal role = (Principal) members.nextElement();
             subjectGroup.addMember(role);
          }
       }
       return true;
    }

    public void initialize(Subject subject, CallbackHandler callbackHandler,
            Map sharedState, Map options)
         {
            this.subject = subject;
            this.callbackHandler = callbackHandler;
            this.sharedState = sharedState;
            this.options = options;
            if( log.isTraceEnabled() )
               log.trace("initialize, instance=@"+System.identityHashCode(this));

            /* Check for password sharing options. Any non-null value for
               password_stacking sets useFirstPass as this module has no way to
               validate any shared password.
            */
            String passwordStacking = (String) options.get("password-stacking");
            if( passwordStacking != null && passwordStacking.equalsIgnoreCase("useFirstPass") )
               useFirstPass = true;

            // Check for a custom Principal implementation
            principalClassName = (String) options.get("principalClass");

            // Check for unauthenticatedIdentity option.
            String name = (String) options.get("unauthenticatedIdentity");
            if( name != null )
            {
               try
               {
                  unauthenticatedIdentity = createIdentity(name);
                  log.trace("Saw unauthenticatedIdentity="+name);
               }
               catch(Exception e)
               {
                  log.warn("Failed to create custom unauthenticatedIdentity", e);
               }
            }
         }

    public boolean logout() throws LoginException
    {
       log.trace("logout");
       // Remove the user identity
       Principal identity = getIdentity();
       Set principals = subject.getPrincipals();
       principals.remove(identity);
       // Remove any added Groups...
       return true;
    }


    /** Find or create a Group with the given name. Subclasses should use this
    method to locate the 'Roles' group or create additional types of groups.
    @return A named Group from the principals set.
    */
   protected Group createGroup(String name, Set principals)
   {
      Group roles = null;
      Iterator iter = principals.iterator();
      while( iter.hasNext() )
      {
         Object next = iter.next();
         if( (next instanceof Group) == false )
            continue;
         Group grp = (Group) next;
         if( grp.getName().equals(name) )
         {
            roles = grp;
            break;
         }
      }
      // If we did not find a group create one
      if( roles == null )
      {
         roles = new GroupImpl(name);
         principals.add(roles);
      }
      return roles;
   }

}
