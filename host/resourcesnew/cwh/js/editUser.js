(function() {
	var cwhApp = angular.module('cwhApp');
	
	cwhApp.controller("EditUserController", function ($scope, $http, $uibModalInstance, title, editUser) {
		$scope.title = title;
		$scope.editUser = editUser;

		$scope.allRoles = [{name:"login"}, {name:"admin"}, {name:"chat"}, {name:"listener"}, {name:"userAdmin"}, {name:"remoteExecution"}];

		$scope.save = function () {
			$uibModalInstance.close(editUser);
		};
		
		$scope.cancel = function () {
			$uibModalInstance.dismiss('cancel');
		};
	})
	
})();