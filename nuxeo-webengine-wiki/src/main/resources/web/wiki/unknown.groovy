//Script called when the object is not found
//Set a 404 error code and render the unknown.ftl template

import javax.servlet.http.HttpServletResponse


Response.setStatus(HttpServletResponse.SC_NO_CONTENT)
Response.setCharacterEncoding('utf-8')

Response.setHeader('Error', 'Yes')


Context.render('/default/unknown.ftl')