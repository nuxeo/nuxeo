msg = "The file has been attached."
Response.sendRedirect("${Context.lastResolvedObject.urlPath}?msg=${msg}")
