
msg="The file has been deleted."
Response.sendRedirect("${Context.getLastResolvedObject().getAbsolutePath()}?msg=${msg}")
