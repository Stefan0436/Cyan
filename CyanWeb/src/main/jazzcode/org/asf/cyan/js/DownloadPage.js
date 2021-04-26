function updateLog(platform, repo, game, modloader) { 
 	$.ajax({
		type: "GET",
        dataType: "json",
		url: "/org.asf.cyan.webcomponents.downloads.DownloadPage/jz:getCompileStats("+platform+", "+repo+", "+game+", "+modloader+")",
		success: function(json) {
			document.getElementById("compilestatus").innerHTML = "Compiler status: " +json.status;
			document.getElementById("log").innerHTML = json.log;
   			document.getElementById("log").scrollTop = document.getElementById("log").scrollHeight;
   			
   			if (document.getElementById("compilestatus").innerHTML === "Compiler status: Done" || document.getElementById("compilestatus").innerHTML === "Compiler status: Fatal Error") {
			document.getElementById("loader").style.display = "none";
				for (var id of intervals)
					window.clearInterval(id);
					
				if (document.getElementById("compilestatus").innerHTML === "Compiler status: Done") {
					document.getElementById("download").style.display = "block";
					document.getElementById("contentd").style.marginTop = "3%";
					if (platform === "paper") {
						document.getElementsByClassName("clientbtn")[0].style.display = "none";
					}
				}
   			}
		}
 	});
}

function runDownloadScripts() {
	if (!(document.getElementById("compilestatus").innerHTML === "Compiler status: Not running" || document.getElementById("compilestatus").innerHTML === "Compiler status: Done")) {
		$(document).ready(function() {
			var element = document.getElementById("compiler-info"); 
		    window.intervals.add(window.setInterval("updateLog('"+element.dataset.platform+"', '"+element.dataset.repository+"', '"+element.dataset.version+"', '"+element.dataset.modloader+"')", 1000));
		});
	} else {
		document.getElementById("log").style.display = "none";
	}
}
