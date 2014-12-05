
if ('serviceWorker' in navigator) {
    var _this = this;
    var pushRegistrationManager;
    navigator.serviceWorker.register( '/sw/serviceworker.js', {scope: '/sw/'} ).then(function() {
        console.log('ServiceWorker Registration succeeded.');
    }).catch(function( error ) {
        console.log('ServiceWorker Registration failed with ', error);
  });

  navigator.serviceWorker.ready.then(function( serviceWorkerRegistration ) {
    console.log("ServiceRegistration:", serviceWorkerRegistration);

    if ('pushRegistrationManager' in serviceWorkerRegistration == false) {
        console.log("pushRegistrationManager not supported. No support for WebPush API, using polyfill.");
        var pushRegistrationManager = new PushRegistrationManager( 'https://localhost:8443' );
        serviceWorkerRegistration.pushRegistrationManager = pushRegistrationManager;
        pushRegistrationManager.connect().then(function() {
            console.log("created push registration manager");
            pushRegistrationManager.monitor();
            pushRegistrationManager.register().then(function( pushRegistration ) {
                console.log("registrationId: ", pushRegistration.registrationId);
                console.log("endpoint: ", pushRegistration.endpoint);
              }, function(error) {
                console.log(error);
              }).catch(function(error) {
                console.log('pushRegistrationManager.register error ', error);
              });
          });
    } else {
        serviceWorkerRegistration.pushRegistrationManager.register().then(function( pushRegistration ) {
            console.log("pushRegistrationManager supported.");
            console.log("registrationId: ", pushRegistration.registrationId);
            console.log("endpoint: ", pushRegistration.endpoint);
          }, function(error) {
            console.log(error);
          }).catch(function(error) {
            console.log('pushRegistrationManager.register error ', error);
          });
    }
    });
} else {
    console.log("No service worker support");
};


