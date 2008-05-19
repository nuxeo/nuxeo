msg = "The file has been deleted."
Response.sendRedirect("${Context.lastResolvedObject.urlPath}?msg=${msg}")
