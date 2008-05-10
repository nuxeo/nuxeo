
msg="The document has been updated successfully."
Response.sendRedirect("${Context.getLastResolvedObject().getUrlPath()}?msg=${msg}")
