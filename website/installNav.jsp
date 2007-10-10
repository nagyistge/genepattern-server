<%--
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2008) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
--%>


<%@ page contentType="text/html" language="Java" import="java.net.URLEncoder" %>
<html>
<head>
<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
<link href="skin/favicon.ico" rel="shortcut icon">
<title>GenePattern configuration</title>
</head>
<body>
<base target="aStep">
<b>Configure your
<img src="skin/HeaderLogo.jpg" width="115" height="17" border="0" alt="GenePattern logo" />
server:</b>
<ol>
<li><a href="installLog.jsp">View the server installation report</a><br><br></li>
<li><a href="pages/taskCatalog.jsf">Install modules</a> from among those at the Broad public website.
	If you want to install all available modules, click the <b>install checked</b> button.<br><br></li>
<li><a href="<%= System.getProperty("JavaGEInstallerURL") %>?version=<%= System.getProperty("GenePatternVersion") %>&server=<%= URLEncoder.encode(request.getScheme()+"://" + request.getServerName() + ":" + request.getServerPort()) %>">Install the Desktop Client</a> (optional)<br><br></li>
<li><a href="installDatasets.htm">Download sample datasets</a><br><br></li>
<li><a href="pages/login.jsf?referrer=index.jsp" target="_parent">Login and begin using GenePattern</a></li>
</ol>

Problems? Questions?  Suggestions? <a href="mailto:gp-help@broad.mit.edu">Contact us</a>.
</body>
</html>
