
msg="The file has been attached."
Response.sendRedirect("${Context.getLastResolvedObject().getAbsolutePath()}?msg=${msg}")
