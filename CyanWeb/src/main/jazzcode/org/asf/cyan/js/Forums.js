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
	
	$.ajax({
		method: "GET",
        dataType: "json",
		url: "/org.asf.cyan.FologoutConfirmBtnrums/jz:pullUserInfo()",
		success: function(json) {
			document.getElementById("usernameBox").value = json.username;
			document.getElementById("nicknameBox").value = json.nickname;
		}
	})
}

function changeAccountInfo() {
	account();
	document.getElementById("cancelLogin").classList.toggle("cancelLoginActive");
	document.getElementById("cancelLogin").classList.toggle("cancelLoginInactive");
	document.getElementById("cancelLogin").innerHTML = "Submitting changes...";
	document.getElementById("cancelLogin").disabled = true;
	document.getElementsByClassName("account-details")[0].onclick = function(){}
	if (document.getElementById("image").files.length != 0) {
		url = document.getElementById("imageUpload").action;
	    $.ajax({
	      	url: url,
		    type: 'POST',
		    data: new FormData( document.getElementById("imageUpload") ),
		    processData: false,
		    contentType: false,
	      	success: function() {
				$.ajax({
					method: "POST",
			        dataType: "json",
			        contentType: 'application/x-www-form-urlencoded',
					url: "/org.asf.cyan.Forums/jz:updateUserInfo()",
					data: {
						"nickname": document.getElementById("nicknameBox").value,
						"username": document.getElementById("usernameBox").value
					},
					success: function(json) {
						document.getElementById("nickname").innerHTML = json.nickname;
						$("#account-image").css("background-image", "url('data:image/png;base64, " + json.accountimage+"')");
						document.getElementById("cancelLogin").innerHTML = "Upload completed"
						document.getElementById("cancelLogin").style.backgroundColor = "green";
						setTimeout(function(){
							document.getElementById("cancelLogin").style.backgroundColor = "white";
							setTimeout(function(){
								document.getElementById("cancelLogin").style.backgroundColor = "green";
								setTimeout(function(){
									document.getElementById("cancelLogin").style.backgroundColor = "white";
									setTimeout(function(){
										document.getElementById("cancelLogin").style.backgroundColor = "green";
										setTimeout(function(){
											document.getElementById("cancelLogin").classList.toggle("cancelLoginInactive");
											setTimeout(function() {
											document.getElementById("cancelLogin").classList.toggle("cancelLoginActive");
												document.getElementById("cancelLogin").style.backgroundColor = "white";
									      		document.getElementById("cancelLogin").innerHTML = "Cancel Login";
												document.getElementById("cancelLogin").disabled = false;
											}, 500);
											document.getElementById("image").value = ""
											document.getElementById("imageBtn").innerHTML = "Select file...";
											document.getElementsByClassName("account-details")[0].onclick = function(){
												accountCheck();
											}
										}, 500)	
									}, 500)
								}, 500)						
							}, 500)
						}, 500)
					}
				})
	   		}	
		})
	} else {
		$.ajax({
			method: "POST",
	        dataType: "json",
	        contentType: 'application/x-www-form-urlencoded',
			url: "/org.asf.cyan.Forums/jz:updateUserInfo()",
			data: {
				"nickname": document.getElementById("nicknameBox").value,
				"username": document.getElementById("usernameBox").value
			},
			success: function(json) {
				document.getElementById("nickname").innerHTML = json.nickname;
				$("#account-image").css("background-image", "url('data:image/png;base64, " + json.accountimage+"')'");
				document.getElementById("cancelLogin").innerHTML = "Upload completed"
				document.getElementById("cancelLogin").style.backgroundColor = "green";
				setTimeout(function(){
					document.getElementById("cancelLogin").style.backgroundColor = "white";
					setTimeout(function(){
						document.getElementById("cancelLogin").style.backgroundColor = "green";
						setTimeout(function(){
							document.getElementById("cancelLogin").style.backgroundColor = "white";
							setTimeout(function(){
								document.getElementById("cancelLogin").style.backgroundColor = "green";
								setTimeout(function(){
									document.getElementById("cancelLogin").classList.toggle("cancelLoginInactive");
									setTimeout(function() {
									document.getElementById("cancelLogin").classList.toggle("cancelLoginActive");
										document.getElementById("cancelLogin").style.backgroundColor = "white";
							      		document.getElementById("cancelLogin").innerHTML = "Cancel Login";
										document.getElementById("cancelLogin").disabled = false;
									}, 500);
									document.getElementById("image").value = ""
									document.getElementById("imageBtn").innerHTML = "Select file...";
									document.getElementsByClassName("account-details")[0].onclick = function(){
										accountCheck();
									}
								}, 500)	
							}, 500)
						}, 500)						
					}, 500)
				}, 500)
			}
		})
	}
}

function changePasswd() {
	account()
	document.getElementsByClassName("account-details")[0].onclick = function(){}
	toggleChangePassword()
}

function confirmChangePassword() {
	document.getElementById("changePassForm").submit()
	document.getElementsByClassName("account-details")[0].onclick = function(){
		accountCheck();
	}
	toggleChangePassword()
}

function cancelChangePasswd() {
	document.getElementById("changePassForm").reset()
	document.getElementsByClassName("account-details")[0].onclick = function(){
		accountCheck();
	}
	toggleChangePassword()
}

function logoutBtn() {
	account();
	toggleLogout();
}

function logoutConfirmBtn() {
	toggleLogout();
	navDirect(window.location + "&logout=true&returnurl=" + encodeURIComponent(window.location))
}

function toggleLogout() {
	document.getElementById("confirmlogout").classList.toggle("activeLogout");
	document.getElementById("confirmlogout").classList.toggle("inactiveLogout");
	document.getElementById("logoutTitle").classList.toggle("activeLogoutTitle");
	document.getElementById("logoutTitle").classList.toggle("inactiveLogoutTitle");
	document.getElementById("cancelLogout").classList.toggle("cancelLogoutInactive");
	document.getElementById("cancelLogout").classList.toggle("cancelLogoutActive");
	document.getElementById("confirmLogoutBtn").classList.toggle("confirmLogoutBtnInactive");
	document.getElementById("confirmLogoutBtn").classList.toggle("confirmLogoutBtnActive");
}

$(document).ready(function(){ 
	elements = document.getElementById("changePassword").children
	for (ele of elements) {
		ele.classList.toggle("inactiveUIElement");
	}
})

function toggleChangePassword() {
	document.getElementById("changePassword").classList.toggle("activeChangePassword");
	document.getElementById("changePassword").classList.toggle("inactiveUI");
	elements = document.getElementById("changePassword").children
	for (ele of elements) {
		ele.classList.toggle("inactiveUIElement");
		ele.classList.toggle("activeUIElement");
	}
}

function cancelLogin() {
	document.getElementById("loginFrame").classList.toggle("loginFrameInactive");
	if (document.getElementById("is-amas") == null) {
		document.getElementById("cancelLogin").classList.toggle("cancelLoginInactive");
		document.getElementById("cancelLogin").classList.toggle("cancelLoginActive");
	}
	setTimeout(function() { 
		document.getElementById("loginFrame").classList.toggle("loginFrameActive");
	}, 500);
}
