msg = "The file has been attached."
Response.sendRedirect("${Context.targetObject.urlPath}?msg=${msg}")
