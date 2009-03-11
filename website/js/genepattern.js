// toggleCheckBoxes -- used in combination with a "master" checkbox to toggle
// the state of a collection of child checkboxes.  Assumes the children and parent
// share a common container parent    
function toggleCheckBoxes(maincheckbox, parentId) {	
  var isChecked = maincheckbox.checked;
  var parentElement = document.getElementById(parentId);   
  var elements = parentElement.getElementsByTagName("input");
  for (i = 0; i < elements.length; i++) {	
    if(elements[i].type="checkbox") {		
      elements[i].checked = isChecked;
    }
  }
}


//
// stop a running job by its ID
//
function stopJob(button, jobId) {
    var really = confirm('Really stop this Job?');
    if (!really) return;
    window.open("runPipeline.jsp?cmd=stop&jobID="+jobId, "_blank", "height=100, width=100, directories=no, menubar=no, statusbar=no, resizable=no");
}

//request email notification for a user and a job
function requestEmailNotification(userEmail, userId, jobId) {
    var opt = {
            method:    'post',
            postBody:  'cmd=notifyEmailJobCompletion&userEmail='+userEmail+'&userID='+userId+'&jobID='+jobId,
            onSuccess: ajaxEmailResponse,
            onFailure: function(t) {
                alert('Error ' + t.status + ' -- ' + t.statusText);
            }
    } 
    new  Ajax.Request('/gp/notifyJobCompletion.ajax',opt);
}

function cancelEmailNotification(userEmail, userId, jobId) {  
    var opt = { 
            method:    'post',
            postBody:  'cmd=cancelEmailJobCompletion&userEmail='+userEmail+'&userID='+userId+'&jobID='+jobId,
            onSuccess: ajaxEmailResponse,
            onFailure: function(t) {
                alert('Error ' + t.status + ' -- ' + t.statusText);
            }
    } 
    new  Ajax.Request('/gp/cancelJobCompletion.ajax',opt);  
}

  function ajaxEmailResponse( req ) {

        if (req.readyState == 4) {
          if (req.status == 200) { // only if "OK"
            //alert('all is well on email submission') 
          } else {
            alert("There was a problem in email notification:\n" + req.statusText);
          }  
        }
      }

      
   
   
    
   // Sends an asychronous request to the managed bean specified by the elExpression (e.g. jobsBean.taskCode).
   // elExpression The expression.
   // parameters The parameters to send to the bean method.
   // callbackFunction The function to invoke when a response is received from the server.
   // method Either post or get.
   
	function sendAjaxRequest(elExpression, parameters, callbackFunction, method, ajaxServletUrl) {
        var opt = {
          method: method,
          postBody: parameters + '&el=' + elExpression,
          onSuccess: callbackFunction,
          onFailure: function(t) {
            alert('Error ' + t.status + ' -- ' + t.statusText);
          }
        }
        new Ajax.Request(ajaxServletUrl, opt);
	}
	 
	// Gets the form parameters for the form with the specified form id.
	// The form parameters as a string.
	
	function getFormParameters(formId) {
		var form = $(formId);
		if(form == null) {
			alert("Form " + formId + " not found.");
		}
		var params = "";
		for(var i = 0; i < form.elements.length; i++) {
			if(i > 0) {
				params += "&";
			}
			var e = form.elements[i];
			var val = e.value;
			if(e.type=='checkbox') {
				val = e.checked ? "on" : "";
			} 
			params += e.name + "=" + val;
		}
		return params;
	}
	

   