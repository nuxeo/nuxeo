response = req.getResponse()
msg="The file has been attached."
response.sendRedirect("${req.getLastResolvedObject().getAbsolutePath()}?msg=${msg}")
