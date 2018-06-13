#import "CsIPay88.h"


@interface CsIPay88 ()

@property Ipay *paymentSdk;
@property UIView *paymentView;
@property NSString *callbackId;
@property bool paymentInProgress;

@end


@implementation CsIPay88

@synthesize paymentSdk;
@synthesize paymentView;
@synthesize callbackId;
@synthesize paymentInProgress;

// Helper
- (void)sendResult: (CDVPluginResult*)result
{
  self.paymentInProgress = false;
  [self.commandDelegate sendPluginResult:result callbackId:self.callbackId];
}

// makepayment entry point
- (void)makepayment: (CDVInvokedUrlCommand*)command
{
  self.callbackId = [command callbackId];

  if(self.paymentInProgress) {
    [self sendResult: [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"A payment is already in progress."]];
    return;
  }
  self.paymentInProgress = true;

  NSDictionary *args = (NSDictionary*) [command argumentAtIndex:0 withDefault:nil andClass:[NSDictionary class]];
  if(args == nil) {
    [self sendResult: [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Argument must be an object."]];
    return;
  }
  
  // Mandatory arguments
  NSNumber *amount = [args objectForKey:@"amount"];
  NSString *name = [args objectForKey:@"name"];
  NSString *email = [args objectForKey:@"email"];
  NSString *phone = [args objectForKey:@"phone"];
  NSString *refNo = [args objectForKey:@"refNo"];
  NSString *currency = [args objectForKey:@"currency"];
  NSString *country = [args objectForKey:@"country"];
  NSString *description = [args objectForKey:@"description"];
  NSString *remark = [args objectForKey:@"remark"];
  NSString *paymentId = [args objectForKey:@"paymentId"];
  NSString *lang = [args objectForKey:@"lang"];
  NSString *merchantKey = [args objectForKey:@"merchantKey"];
  NSString *merchantCode = [args objectForKey:@"merchantCode"];
  NSString *backendPostUrl = [args objectForKey:@"backendPostUrl"];
  if(amount == nil || name == nil || email == nil || phone == nil || refNo == nil
    || currency == nil || country == nil || description == nil || remark == nil || paymentId == nil
    || lang == nil || merchantKey == nil || merchantCode == nil
    || backendPostUrl == nil
  ) {
    [self sendResult: [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Required arguments are missing."]];
    return;
  }
  
  // NSString *formattedAmount = [NSString stringWithFormat:@"%.2f", [amount doubleValue]/100.0];
  NSString *formattedAmount = [NSString stringWithFormat:@"%.2f", [amount doubleValue]/1.0];
  
  // Setup iPay88 payment object.
  IpayPayment *payment = [[IpayPayment alloc] init];
  [payment setPaymentId:paymentId];
  [payment setMerchantKey:merchantKey];
  [payment setMerchantCode:merchantCode];
  [payment setRefNo:refNo];
  [payment setAmount:formattedAmount];
  [payment setCurrency:currency];
  [payment setProdDesc:description];
  [payment setUserName:name];
  [payment setUserEmail:email];
  [payment setUserContact:phone];
  [payment setRemark:remark];
  [payment setLang:lang];
  [payment setCountry:country];
  [payment setBackendPostURL:backendPostUrl];
  
  // Create iPay88 View.
  self.paymentSdk = [[Ipay alloc] init];
  self.paymentSdk.delegate = self;
  self.paymentView = [self.paymentSdk checkout:payment];
  
  // Transfer control to iPay88 View.
  [self.webView addSubview:self.paymentView];
}


/** iPay88 Result Delegate **/

- (void)paymentSuccess:(NSString *)refNo withTransId:(NSString *)transId withAmount:(NSString *)amount withRemark:(NSString *)remark withAuthCode:(NSString *)authCode
{
  [self.paymentView removeFromSuperview];
  NSArray *keys = [NSArray arrayWithObjects:@"transactionId", @"referenceNo", @"amount", @"remarks", @"authCode", nil];
  NSArray *objects = [NSArray arrayWithObjects:transId, refNo, amount, remark, authCode, nil];
  NSDictionary *result = [NSDictionary dictionaryWithObjects:objects forKeys:keys];
  [self sendResult: [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result]];
}

- (void)paymentFailed:(NSString *)refNo withTransId:(NSString *)transId withAmount:(NSString *)amount withRemark:(NSString *)remark withErrDesc:(NSString *)errDesc
{
  [self.paymentView removeFromSuperview];
  NSArray *keys = [NSArray arrayWithObjects:@"transactionId", @"referenceNo", @"amount", @"remarks", @"err", nil];
  NSArray *objects = [NSArray arrayWithObjects:transId, refNo, amount, remark, errDesc, nil];
  NSDictionary *result = [NSDictionary dictionaryWithObjects:objects forKeys:keys];
  [self sendResult: [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:result]];
}

- (void)paymentCancelled:(NSString *)refNo withTransId:(NSString *)transId withAmount:(NSString *)amount withRemark:(NSString *)remark withErrDesc:(NSString *)errDesc
{
  [self.paymentView removeFromSuperview];
  NSArray *keys = [NSArray arrayWithObjects:@"transactionId", @"referenceNo", @"amount", @"remarks", @"err", nil];
  NSArray *objects = [NSArray arrayWithObjects:transId, refNo, amount, remark, @"canceled", nil];
  NSDictionary *result = [NSDictionary dictionaryWithObjects:objects forKeys:keys];
  [self sendResult: [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:result]];
}

@end
