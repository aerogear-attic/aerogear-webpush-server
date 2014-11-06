// register service worker

if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('./serviceworker.js', {
    scope: '/'
  }).then(function() {
    console.log('ServiceWorker Registration succeeded.');
  }).catch(function(error) {
    console.log('ServiceWorker Registration failed with ', error);
  });

  navigator.serviceWorker.ready.then( function(serviceWorkerRegistration) {
    serviceWorkerRegistration.pushRegistrationManager.register().then(
      function(pushRegistration) {
        console.log(pushRegistration.registrationId);
        console.log(pushRegistration.endpoint);
        // TODO: register with app server.
      }, function(error) {
        console.log(error);
      }).catch(function(error) {
        console.log('pushRegistrationManager.register error ', error);
      });
    });
};

window.onload = function() {
};
