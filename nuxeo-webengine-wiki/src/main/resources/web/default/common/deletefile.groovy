response = req.getResponse()
msg="The file has been deleted."
response.sendRedirect("${req.getLastResolvedObject().getAbsolutePath()}?msg=${msg}")
