import org.nuxeo.runtime.api.Framework;
import org.nuxeo.ecm.platform.usermanager.UserManager;

def main () {
    query = Request.getParameter("query");
    gquery = Request.getParameter("gquery");
    userManager = Framework.getService(UserManager.class)

    results = null
    if (query) {
        results = userManager.searchPrincipals(query)
        Context.render("/users/users.ftl", ['users': results])
    } else if (gquery) {
        results = userManager.searchGroups(gquery)
        Context.render("/users/users.ftl", ['groups': results])
    } else {
        Context.render("/users/users.ftl")
    }
}

main()
