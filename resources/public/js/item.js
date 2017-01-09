(function() {
	function deleteItem(id, elementId, popupId) {
	    if (confirm('Delete this item? It will be gone forever.')) {
			document.getElementById(elementId).style.display = 'none';
			document.getElementById(popupId).style.display = 'none';
			$.ajax({ url: '/i/' + id, type: 'DELETE' });
	    }
	}

	function showItemExtras(id) {
	    document.getElementById(id).style.display = 'block';
	}

	function hideItemExtras(element) {
        element.style.display = 'none';
	}

	function registerCallbacks() {
		$('.delete-item-link').click(function(evt) {
			var id = $(evt.currentTarget).data('item-id');
			deleteItem(id, 'item-' + id, 'item-' + id + '-extras');
			evt.preventDefault();
		});

		$('span.item-extra').click(function(evt) {
			var id = $(evt.currentTarget).data('item-id');
			showItemExtras('item-' + id + '-extras');
		});

		$('div.item-extras-w').hide();
		$('div.item-extras-w').click(function(evt) {
			hideItemExtras(evt.currentTarget);
		});

		$('div.item-extras').click(function(evt) {
			evt.stopPropagation();
		});
	}

	$(registerCallbacks);
})();