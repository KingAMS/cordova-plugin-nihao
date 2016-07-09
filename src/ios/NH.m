/* ModusEcho.m */

#import "NH.h"
#import "CDVFile.h"
#import "NSData+Base64.h"

@implementation NH

/*
- (void)echo:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult* pluginResult = nil;
    NSString* msg = [command.arguments objectAtIndex:0];

    if (msg == nil || [msg length] == 0) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    } else {
        // http://stackoverflow.com/questions/18680891/displaying-a-message-in-ios-which-has-the-same-functionality-as-toast-in-android
        UIAlertView *toast = [
            [UIAlertView alloc] initWithTitle:@""
            message:msg
            delegate:nil
            cancelButtonTitle:nil
            otherButtonTitles:nil, nil];

        [toast show];

        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 3 * NSEC_PER_SEC), dispatch_get_main_queue(), ^{
            [toast dismissWithClickedButtonIndex:0 animated:YES];
        });

        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:msg];
    }

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
*/

- (void)cropAndResize:(CDVInvokedUrlCommand*)command {

  NSDictionary *options = [command.arguments objectAtIndex:0];

  CGFloat width = [[options objectForKey:@"width"] floatValue];
  CGFloat height = [[options objectForKey:@"height"] floatValue];
  NSInteger quality = [[options objectForKey:@"quality"] integerValue];
  NSString *format =  [options objectForKey:@"format"];
  NSString *filename =  [options objectForKey:@"filename"];
  bool storeImage = [[options objectForKey:@"storeImage"] boolValue];

  //Load the image
  UIImage *img = [self getImageUsingOptions:options];
  UIImage *scaledImage = [self cropResizeImage:img toWidth:width toHeight:height];

  NSNumber *newWidthObj = [[NSNumber alloc] initWithFloat:width];
  NSNumber *newHeightObj = [[NSNumber alloc] initWithFloat:height];

  CDVPluginResult* pluginResult = nil;
  if (storeImage) {
    bool written = [self saveImage:scaledImage withOptions:options];
    if (written) {
        NSDictionary* result = [NSDictionary dictionaryWithObjects:[NSArray arrayWithObjects:filename, newWidthObj, newHeightObj, nil] forKeys:[NSArray arrayWithObjects: @"filename", @"width", @"height", nil]];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    }
  } else {
    NSData* imageDataObject = nil;
    if ([format isEqualToString:@"jpg"]) {
      imageDataObject = UIImageJPEGRepresentation(scaledImage, (quality/100.f));
    } else {
      imageDataObject = UIImagePNGRepresentation(scaledImage);
    }

    NSString *encodedString = [imageDataObject base64EncodingWithLineLength:0];
    NSDictionary* result = [NSDictionary dictionaryWithObjects:[NSArray arrayWithObjects:encodedString, newWidthObj, newHeightObj, nil] forKeys:[NSArray arrayWithObjects: @"imageData", @"width", @"height", nil]];

    if (encodedString != nil) {
      pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
    } else {
      pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    }
  }
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (UIImage*) getImageUsingOptions:(NSDictionary*)options {
  NSString *imageUri = [options objectForKey:@"imageUri"];

  NSURL *url = [NSURL URLWithString:imageUri];
  NSData * data = [NSData dataWithContentsOfURL:url];
  UIImage * img = [UIImage imageWithData:data];

  return img;
}

-(UIImage *)cropResizeImage:(UIImage *)sourceImage toWidth:(CGFloat)newWidth toHeight:(CGFloat)newHeight {
  // input size comes from image
  CGSize inputSize = sourceImage.size;

  // round up side length to avoid fractional output size
  newWidth = ceilf(newWidth);
  newHeight = ceilf(newHeight);

  // output size has sideLength for both dimensions
  CGSize outputSize = CGSizeMake(newWidth, newHeight);

  // calculate scale so that smaller dimension fits sideLength
  CGFloat scale = MAX(newWidth / inputSize.width,
                      newHeight / inputSize.height);

  // scaling the image with this scale results in this output size
  CGSize scaledInputSize = CGSizeMake(inputSize.width * scale,
                                      inputSize.height * scale);

  // determine point in center of "canvas"
  CGPoint center = CGPointMake(outputSize.width/2.0,
                                outputSize.height/2.0);

  // calculate drawing rect relative to output Size
  CGRect outputRect = CGRectMake(center.x - scaledInputSize.width/2.0,
                                  center.y - scaledInputSize.height/2.0,
                                  scaledInputSize.width,
                                  scaledInputSize.height);

  // begin a new bitmap context, scale 0 takes display scale
  UIGraphicsBeginImageContextWithOptions(outputSize, YES, 0);

  // optional: set the interpolation quality.
  // For this you need to grab the underlying CGContext
  CGContextRef ctx = UIGraphicsGetCurrentContext();
  CGContextSetInterpolationQuality(ctx, kCGInterpolationHigh);

  // draw the source image into the calculated rect
  [sourceImage drawInRect:outputRect];

  // create new image from bitmap context
  UIImage *outImage = UIGraphicsGetImageFromCurrentImageContext();

  // clean up
  UIGraphicsEndImageContext();

  // pass back new image
  return outImage;
}

- (bool) saveImage:(UIImage *)img withOptions:(NSDictionary *) options {
    NSString *format =  [options objectForKey:@"format"];
    NSString *filename =  [options objectForKey:@"filename"];
    NSString *directory =  [options objectForKey:@"directory"];
    directory = [self getUrl:directory];
    NSInteger quality = [[options objectForKey:@"quality"] integerValue];
    bool photoAlbum = [[options objectForKey:@"photoAlbum"] boolValue];
    if (photoAlbum == YES) {
        UIImageWriteToSavedPhotosAlbum(img, self, @selector(imageSavedToPhotosAlbum:didFinishSavingWithError:contextInfo:), nil);
        return true;
    } else {
        NSData* imageDataObject = nil;
        if ([format isEqualToString:@"jpg"]) {
            imageDataObject = UIImageJPEGRepresentation(img, (quality/100.f));
        } else {
            imageDataObject = UIImagePNGRepresentation(img);
        }

        NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
        NSString *documentsDirectory = [paths objectAtIndex:0];

        NSMutableString* fullFileName;
        if (![directory isEqualToString:@""]) {
            fullFileName = [NSMutableString stringWithString: directory];
            if (![[NSFileManager defaultManager] fileExistsAtPath:fullFileName]) {
                NSError *error = nil;
                [[NSFileManager defaultManager] createDirectoryAtPath:fullFileName withIntermediateDirectories:NO attributes:nil error:&error];
            }
        } else {
            fullFileName = [NSMutableString stringWithString: documentsDirectory];
        }

        [fullFileName appendString:@"/"];
        [fullFileName appendString:filename];
        NSRange r = [filename rangeOfString:format options:NSCaseInsensitiveSearch];
        if (r.location == NSNotFound) {
            [fullFileName appendString:@"."];
            [fullFileName appendString:format];
        }
        NSError *error = nil;
        bool written = [imageDataObject writeToFile:fullFileName options:NSDataWritingAtomic error:&error];
        if (!written) {
            NSLog(@"Write returned error: %@", [error localizedDescription]);
        }
        return written;
    }
}

- (void) imageSavedToPhotosAlbum:(UIImage *)image didFinishSavingWithError:(NSError *)error contextInfo:(void *)contextInfo {
    NSString *message;
    NSString *title;
    if (!error) {
        title = NSLocalizedString(@"Image Saved", @"");
        message = NSLocalizedString(@"The image was placed in your photo album.", @"");
    }
    else {
        title = NSLocalizedString(@"Error", @"");
        message = [error description];
    }
    UIAlertView *alert = [[UIAlertView alloc] initWithTitle:title
                                                    message:message
                                                   delegate:nil
                                          cancelButtonTitle:@"OK"
                                          otherButtonTitles:nil];
    [alert show];
}

- (NSString *)getUrl:(NSString *)urlString
{
    NSString *path = nil;
    id filePlugin = [self.commandDelegate getCommandInstance:@"File"];
    if (filePlugin != nil) {
        CDVFilesystemURL* url = [CDVFilesystemURL fileSystemURLWithString:urlString];
        path = [filePlugin filesystemPathForURL:url];
    }
    if (path == nil) {
        if ([urlString hasPrefix:@"file:"]) {
            path = [[NSURL URLWithString:urlString] path];
        }
    }
    return path;
}

@end
