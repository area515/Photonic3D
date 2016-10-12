(function() {
	var cwhApp = angular.module('cwhApp');
	cwhApp.controller("UsersController", ['$scope', '$http', '$location', '$anchorScroll', '$uibModal', 'cacheControl', function ($scope, $http, $location, $anchorScroll, $uibModal, cacheControl) {
		$scope.refreshUsers = function refreshUsers() {
	        $http.get('/services/users/list').success(function(data) {
	        	$scope.users = data;
	        });
	    }
		
		$scope.openSaveUserDialog = function openSaveUserDialog(editTitle, isNewUser) {
			var editUserModal = $uibModal.open({
		        animation: true,
		        templateUrl: 'editUser.html',
		        controller: 'EditUserController',
		        size: "lg",
		        resolve: {
		        	title: function () {return editTitle;},
		        	editUser: function () {return $scope.editUser;}
		        }
			});
		    editUserModal.result.then(function (savedUser) {$scope.saveUser(savedUser, isNewUser)});
		}
		
		$scope.changeCurrentUser = function changeCurrentUser(newUser) {
			$scope.currentUser = newUser;
		}
		
		$scope.deleteCurrentUser = function deleteCurrentUser() {
			$http.delete("/services/users/" + $scope.currentUser.userId).success(function (data) {
		        $scope.currentUser = null;
				$scope.refreshUsers();
			}).error(function (data, status, headers, config, statusText) {
     			$scope.$emit("HTTPError", {status:status, statusText:data});
    		});
		}
		
		$scope.editCurrentUser = function editCurrentUser(editTitle) {
			$scope.editUser = JSON.parse(JSON.stringify($scope.currentUser));
			$scope.openSaveUserDialog(editTitle, false);
		}
		
		$scope.saveUser = function saveUser(user, isNewUser) {
			$http.post("/services/users", user).success(function (data) {
				$scope.refreshUsers();
				user.credential = null;
				$scope.currentUser = user;
		        $scope.editUser = null;
			}).error(function (data, status, headers, config, statusText) {
     			$scope.$emit("HTTPError", {status:status, statusText:data});
    		});
		}
		
		$scope.createNewUser = function createNewUser(editTitle) {
			if ($scope.currentUser == null) {
				$scope.editUser = {
						name : "New User",
						remote : false,
						credential : null,
						roles : ["login"],
						email : null,
				};
			} else {
				$scope.editUser = JSON.parse(JSON.stringify($scope.currentUser));
				$scope.editUser.name = $scope.editUser.name + " (Copy)";
				$scope.editUser.credential = null;
				$scope.editUser.remote = false;
			}
			
			$scope.openSaveUserDialog(editTitle, true);
		}
		
		$scope.refreshUsers();
	}])
})();
