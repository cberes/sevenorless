(function() {
	function getFollowUrl(username, action) {
		return '/u/' + username + '/' + action;
	}

	function follow(element, username) {
		var followText = 'Follow';
		if (element.innerHTML === followText) {
			element.innerHTML = 'Unfollow';
			$.post(getFollowUrl(username, 'follow'));
		} else {
			element.innerHTML = followText;
			$.post(getFollowUrl(username, 'unfollow'));
		}
	}

	function approve(element, username) {
		element.innerHTML = 'Approved';
		$.post(getFollowUrl(username, 'approve'));
	}

	function deny(element, username) {
		element.innerHTML = 'Denied';
		$.post(getFollowUrl(username, 'deny'));
	}

	function registerCallbacks() {
		$('#follow').click(function(evt) {
			var element = evt.currentTarget;
			follow(element, $(element).data('username'));
			evt.preventDefault();
		});

		$('#approve').click(function(evt) {
			var element = evt.currentTarget;
			approve(element, $(element).data('username'));
			evt.preventDefault();
		});

		$('#deny').click(function(evt) {
			var element = evt.currentTarget;
			deny(element, $(element).data('username'));
			evt.preventDefault();
		});
	}

	$(registerCallbacks)
})();