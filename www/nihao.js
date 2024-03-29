


exports.checkTTS = function (success, fail, locales) {
    cordova.exec(success, fail, 'NH', 'checktts', locales);
};


exports.cropAndResize = function (success, fail, imageUri, width, height,options) {
    if (!options) {
        options = {};
    }

    var params = {
        imageUri: imageUri,
        width: width ? width : 0,
        height: height ? height : 0,
        format: options.format ? options.format : "jpg",
        quality: options.quality ? options.quality : 75,
        storeImage: options.storeImage ? options.storeImage : false,
        directory: options.directory ? options.directory : null,
        filename: options.filename ? options.filename : null,
    }

    cordova
        .exec(success, fail, 'NH', 'cropAndResize', [params]);
};
