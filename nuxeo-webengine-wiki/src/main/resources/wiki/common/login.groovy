import javax.servlet.http.HttpServletResponse;

user = Session.principal

if (user.isAnonymous()) {
    Response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Login failed");
} else {
    Response.setStatus(200)
    Context.print("Authenticated")
}