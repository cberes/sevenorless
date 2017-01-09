(function() {
	function toggleTinyMce(element, id) {
		if (element.checked) {
			tinymce.execCommand('mceRemoveEditor', false, id);
		} else {
			tinymce.execCommand('mceAddEditor', false, id);
		}
	}

	function initToggles() {
		$('input.tinymce-toggle').change(function(evt) {
			var element = evt.currentTarget;
			var id = $(element).data('editor-id');
			toggleTinyMce(element, id);
		});
	}

	$(initToggles);
})();