(function() {
	function toggleComments(id, elementId) {
		var element = document.getElementById(elementId);
		if (element.style.display == 'none') {
			element.style.display = 'block';
			var placeholders = element.getElementsByClassName('loading');
			if (placeholders.length > 0) {
				var ph = placeholders.item(0).parentNode;
				$.get('/comments/' + id, function(data) {
					ph.innerHTML = data;
				});
			}
		} else {
			element.style.display = 'none';
		}
	}

	function addComment(id, elementId, form) {
		var element = document.getElementById(elementId);
		var placeholders = element.getElementsByClassName('comments-placeholder');
		var ph = placeholders.item(0);
		$.post('/comments/' + id, $(form).serialize()).done(function(data) {
			ph.innerHTML = data;
		});
		form.reset();
	}

	$(function() {
		$('div.m').click(function(evt) {
			var element = evt.currentTarget;
			var id = $(element).data('item-id');
			toggleComments(id, "comments-" + id);
		});

		$('form.comment').submit(function(evt) {
			var element = evt.currentTarget;
			var id = $(element).data('item-id');
			addComment(id, "comments-" + id, element);
			evt.preventDefault();
		});
	});
})();
