
msg="Your document has been created successfully."
Response.sendRedirect("${Context.getLastResolvedObject().getUrlPath()}?msg=${msg}")
