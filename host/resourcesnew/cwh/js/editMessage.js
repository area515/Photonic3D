(function() {
	var cwhApp = angular.module('cwhApp');
	
	cwhApp.controller("EditMessageController", function ($scope, $http, $uibModalInstance, title, editMessage, users) {
		$scope.users = users;
		$scope.title = title;
		$scope.editMessage = editMessage;
		
		$scope.save = function () {
			$uibModalInstance.close(editMessage);
		};
		
		$scope.cancel = function () {
			$uibModalInstance.dismiss('cancel');
		};
	})
	
})();