
msg="The file has been deleted."
Response.sendRedirect("${Context.getLastResolvedObject().getUrlPath()}?msg=${msg}")
