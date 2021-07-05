function updateDownload(platform, repo, game, modloader) { 
 	$.ajax({
		type: "GET",
        dataType: "json",
		url: "/org.asf.cyan.webcomponents.downloads.DownloadPage/jz:getZipInfo(platform: "+platform+", repository: "+repo+", version: "+game+", modloader: "+modloader+")",
		success: function(json) {
			if (json.status === "in_progress") {
				document.getElementsByClassName("sourcesbtn")[0].innerHTML = "Creating source zip...";
			} else if (json.status === "done") {
				document.getElementsByClassName("sourcesbtn")[0].id = "downloads-btn";;
				document.getElementsByClassName("sourcesbtn")[0].innerHTML = "Download sources";
				document.getElementsByClassName("sourcesbtn")[0].onclick = function(){
					window.location.href = document.getElementsByClassName("sourcesbtn")[0].dataset.url;
				};
				for (var id of intervals)
					window.clearInterval(id);
			}
		}
 	});
}

function runDownloadScripts() {
	$(document).ready(function() {
		var element = document.getElementById("compiler-info"); 
	    window.intervals.add(window.setInterval("updateDownload('"+element.dataset.platform+"', '"+element.dataset.repository+"', '"+element.dataset.version+"', '"+element.dataset.modloader+"')", 1000));
	});
}
