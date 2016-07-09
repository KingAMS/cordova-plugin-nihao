/* ModusEcho.h */

#import <Cordova/CDV.h>

@interface NH : CDVPlugin {
  NSString* callbackID;
}

@property (nonatomic, copy) NSString* callbackID;

//- (void)echo:(CDVInvokedUrlCommand*)command;
- (void)cropAndResize:(CDVInvokedUrlCommand*)command;
- (bool) saveImage:(UIImage *)image withOptions:(NSDictionary *) options;
- (void) imageSavedToPhotosAlbum:(UIImage *)image didFinishSavingWithError:(NSError *)error contextInfo:(void *)none;

@end
