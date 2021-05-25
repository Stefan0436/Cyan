function toggleMenu(cont) {
	if (cont.classList.contains("change")) {
		for (ele of document.getElementsByClassName("entries")) {
			ele.style.overflowY = "hidden";
		}
		for (ele of document.getElementsByClassName("menu-control")) {
			ele.title = "Show the menu";
		}
		document.getElementsByClassName("sidebar")[0].style.width = "56px";
		document.getElementsByClassName("menu-control")[0].style.left = "10px";
		for (itm of document.getElementsByClassName("header")) {
			itm.classList.toggle("hidden");
			itm.classList.toggle("hiddenDisplay");
		}
		for (itm of document.getElementsByClassName("entry")) {
			itm.classList.toggle("hidden");
			itm.classList.toggle("hiddenDisplay");
		}
	} else {
		for (ele of document.getElementsByClassName("entries")) {
			ele.style.overflowY = "auto";
		}
		for (ele of document.getElementsByClassName("menu-control")) {
			ele.title = "Hide the menu";
		}
		document.getElementsByClassName("sidebar")[0].style.width = "198px";
		document.getElementsByClassName("menu-control")[0].style.left = "84px";	
		for (itm of document.getElementsByClassName("header")) {
			itm.classList.toggle("hidden");
		}
		for (itm of document.getElementsByClassName("entry")) {
			itm.classList.toggle("hidden");
		}
	}
	cont.classList.toggle("change");
}

function account() {
	document.getElementById("accountdetails").classList.toggle("openaccount");
	document.getElementById("accountdetails").classList.toggle("closedaccount");
}

function selectFile() {
	if (document.getElementById("image").files.length == 0) {
		document.getElementById("imageBtn").innerHTML = "Select file...";
	} else {
		document.getElementById("imageBtn").innerHTML = document.getElementById("image").files.length + " file(s) selected.";
	}
}

function resetAccount() {
	document.getElementById("image").value = ""
	document.getElementById("imageBtn").innerHTML = "Select file...";
}