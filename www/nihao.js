


exports.checkTTS = function (locales, onfulfilled, onrejected) {
   

    cordova
        .exec(function (res) {
            onfulfilled(res);
        }, function (reason) {
            onrejected(reason);
        }, 'NH', 'checktts', locales);
};