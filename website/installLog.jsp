<%@ page import="java.io.BufferedReader,
		 java.io.File,
		 java.io.FileReader"
	session="false" contentType="text/html" language="Java" %><%

	response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
	response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
	response.setDateHeader("Expires", 0);

	String filename = "GenePattern_InstallLog.log";
	String FATAL_MARKER = "Status: FATAL";
	String ERROR_MARKER = "Status: ERROR";
	String WARNING_MARKER = "Status: WARNING";
	String ADDITIONAL_NOTES = "Additional Notes: ";
	String DETAIL = "Action Notes:";
	int numErrors = 0;
	File f = new File("..", filename);
%>
<html>
<head>
<link href="stylesheet.css" rel="stylesheet" type="text/css">
<link rel="SHORTCUT ICON" href="favicon.ico" >
<title><%= filename %> from <%= request.getServerName() %></title>
</head>
<body>
<!--jsp:include page="navbar.jsp"></jsp:include> -->
<pre>
<%= f.getCanonicalPath() %> on <%= request.getServerName() %>
<hr>
<%
	// wait for the installer to materialize the log file
	for (int i = 0; i < 30 && !f.exists(); i++) {
		Thread.sleep(1000); // sleep one second
	}
	if (!f.exists()) {
		out.println("Cannot find log file " + f.getCanonicalPath());
		return;
	}

	BufferedReader logFile = new BufferedReader(new FileReader(f));

	String curLine = null;
	// first print the summary
	while ((curLine = logFile.readLine()) != null) {
		if (curLine.indexOf(DETAIL) == -1) {
			out.println(curLine);
		} else {
			break;
		}
	}

	// now print all of the errors, warnings, and fatal messages
	String lastLine = curLine;
	while ((curLine = logFile.readLine()) != null) {
		if (curLine.indexOf(FATAL_MARKER) != -1 ||
		    curLine.indexOf(ERROR_MARKER) != -1 ||
		    curLine.indexOf(WARNING_MARKER) != -1) {
			numErrors++;
			out.print(numErrors + ". ");
			out.println(lastLine);
			curLine = logFile.readLine();
			out.println(curLine.substring(curLine.indexOf(ADDITIONAL_NOTES) + ADDITIONAL_NOTES.length()));
			out.println("");
		}
		lastLine = curLine;
	}
	logFile.close();
%>
</pre>
<script language="javascript">
//	if (<%= numErrors %> == 0) window.close();
</script>
<!--jsp:include page="footer.jsp"></jsp:include-->
</body>
</html>
