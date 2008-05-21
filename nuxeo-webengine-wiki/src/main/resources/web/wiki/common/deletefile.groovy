msg = "The file has been deleted."
Response.sendRedirect("${Context.targetObject.urlPath}?msg=${msg}")
