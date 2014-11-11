import org.nuxeo.runtime.api.Framework;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl

def main () {
    username = Request.getParameter("username");
    userManager = Framework.getService(UserManager.class)
    if (username) {
        user = userManager.getPrincipal(username)
        if (user) {
            // update
            user.firstName = Request.getParameter("firstName")
            user.lastName = Request.getParameter("lastName")
            user.password = Request.getParameter("password")
            
            selectedGroups = Request.getParameterValues("groups")
            listGroups = Arrays.asList(selectedGroups)
            user.setGroups(listGroups)
            
            userManager.updatePrincipal(user)
        } else {
            // create
            user = new NuxeoPrincipalImpl(Request.getParameter("username"))
            user.firstName = Request.getParameter("firstName")
            user.lastName = Request.getParameter("lastName")
            user.password = Request.getParameter("password")
            
            selectedGroups = Request.getParameterValues("groups")
            listGroups = Arrays.asList(selectedGroups)
            user.setGroups(listGroups)
            
            userManager.createPrincipal(user)
        }
    }

    Context.redirect(appPath + "/users")
}

main()
