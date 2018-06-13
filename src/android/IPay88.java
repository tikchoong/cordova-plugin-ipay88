// This code can only handle one payment at any given moment - no concurrent payments
// are allowed!

package org.cloudsky.cordovaPlugins;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;

import com.ipay.IPayIH;
import com.ipay.IPayIHPayment;
import com.ipay.IPayIHResultDelegate;
import com.ipay.IPayIHR;

import android.content.Intent;
import java.util.Locale;
import java.io.Serializable;

public class IPay88 extends CordovaPlugin {
    
    // Configuration. CUSTOMISE THIS ACCORDING TO YOUR NEEDS! ----------
    //public static int IPAY88_ACT_REQUEST_CODE = 88;
    // ipay88 doc uses 1
    public static int IPAY88_ACT_REQUEST_CODE = 1;
    public final String TAG = "iPay88 Plugin";

    // iPay88 results receiver -----------------------------------------

    static public class ResultDelegate implements IPayIHResultDelegate, Serializable {
        //dun have in ipay88 doc
        private static final long serialVersionUID = 5963066398271211659L;
        
        public final static int STATUS_OK = 1;
        public final static int STATUS_FAILED = 2;
        public final static int STATUS_CANCELED = 0;
        
        public void onPaymentSucceeded (String transId, String refNo, String amount, String remarks, String auth, String ccName,
                                   String ccNo, String sBankname, String sCountry)
        {
        	Log.i("### ResultDelegate ", "Success");
            IPay88.r_status = STATUS_OK;
            IPay88.r_transactionId = transId;
            IPay88.r_referenceNo = refNo;
            IPay88.r_amount = amount;
            IPay88.r_remarks = remarks;
            IPay88.r_authCode = auth;
            IPay88.r_ccNo = ccNo;
            IPay88.r_ccName = ccName;
            IPay88.r_sBankName = sBankname;
            IPay88.r_sCountry = sCountry;
        }
        
        public void onPaymentFailed (String transId, String refNo, String amount, String remarks, String err, String ccName,
                                   String ccNo, String sBankname, String sCountry)
        {
        	Log.i("### ResultDelegate ", "Failed");
            IPay88.r_status = STATUS_FAILED;
            IPay88.r_transactionId = transId;
            IPay88.r_referenceNo = refNo;
            IPay88.r_amount = amount;
            IPay88.r_remarks = remarks;
            IPay88.r_err = err;
            IPay88.r_ccNo = ccNo;
            IPay88.r_ccName = ccName;
            IPay88.r_sBankName = sBankname;
            IPay88.r_sCountry = sCountry;
        }

        public void onPaymentCanceled (String transId, String refNo, String amount, String remarks, String errDesc, String ccName,
                                   String ccNo, String sBankname, String sCountry)
        {
        	Log.i("### ResultDelegate ", "Cancelled");
            IPay88.r_status = STATUS_CANCELED;
            IPay88.r_transactionId = transId;
            IPay88.r_referenceNo = refNo;
            IPay88.r_amount = amount;
            IPay88.r_remarks = remarks;
            IPay88.r_err = "canceled";
            IPay88.r_ccNo = ccNo;
            IPay88.r_ccName = ccName;
            IPay88.r_sBankName = sBankname;
            IPay88.r_sCountry = sCountry;
        }

        public void onConnectionError(String merchantCode, String merchantKey,
                                  String refNo, String amount, String remark, String lang, String country) {
        	IPay88.r_merchantCode = merchantCode;
        	IPay88.r_merchantKey = merchantKey;
        	IPay88.r_referenceNo = refNo;
        	IPay88.r_amount = amount;
        	IPay88.r_remarks = remark;
        	IPay88.r_lang = lang;
        	IPay88.r_sCountry = country;
        }

        public void onRequeryResult (String merchantCode, String refNo, String amount, String result)
        {
            // TODO warning, this is a stub to satisfy superclass interface
            // requirements. We do not yet have any meaningful support for
            // requery in this Cordova library yet.
            
            // Sam just addin
            IPay88.r_merchantCode = merchantCode;
            IPay88.r_referenceNo = refNo;
            IPay88.r_amount = amount;
            IPay88.r_result = result;
        }
    }

    
    // State -----------------------------------------------------------

    // Class variables to transfer results from IpayResultDelegate.
    public static boolean isInProgress = false;
    public static int     r_status;
    public static String  r_transactionId;
    public static String  r_referenceNo;
    public static String  r_amount;
    public static String  r_remarks;
    public static String  r_authCode;
    public static String  r_err;

    public static String  r_ccNo;
    public static String  r_ccName;
    public static String  r_sBankName;
    public static String  r_sCountry;
    public static String  r_lang;

    private ResultDelegate iPayDelegate;
    private CallbackContext cordovaCallbackContext;
    
    //Sam : add on
    public static String r_merchantCode;
    public static String r_merchantKey;
    public static String r_result;


    // Methods ---------------------------------------------------------
    
    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent intent)
    {
        if(requestCode == IPAY88_ACT_REQUEST_CODE) {
            try {
                JSONObject resp = new JSONObject();
                resp.put("transactionId", r_transactionId);
                resp.put("referenceNo", r_referenceNo);
                resp.put("amount", r_amount);
                resp.put("remarks", r_remarks);
                // sam add
                resp.put("result", r_result);
                switch(r_status) {
                    case ResultDelegate.STATUS_OK:
                        resp.put("authCode", r_authCode);
                        cordovaCallbackContext.success(resp);
                        break;
                    case ResultDelegate.STATUS_FAILED:
                    case ResultDelegate.STATUS_CANCELED:
                        resp.put("err", r_err);
                        cordovaCallbackContext.error(resp);
                        break;
                    default:
                        cordovaCallbackContext.error("Unexpected result from iPay88 plugin.");
                }
            } catch (Exception e) {
                cordovaCallbackContext.error("Unexpected failure in iPay88 plugin: "+e.getMessage());
            } finally {
                isInProgress = false;
            }
        }
    }
    
    @Override
    public boolean execute (String action, JSONArray args, CallbackContext callbackContext)
    throws JSONException
    {
        JSONObject argObj;
        argObj = args.getJSONObject(0);

        Log.i("### Execute", argObj.toString());
                
        if(action.equals("makepayment")) {
        	
        	Log.i("### Execute ", "in progress - " + isInProgress);

            if(isInProgress) {
                callbackContext.error("Another payment is in progress!");
            } else {
                //JSONObject argObj;

                cordovaCallbackContext = callbackContext;
                initCanceledResult(); // Default the result to "canceled", as ResultDelegate is not called on backbutton exit.

                //argObj = args.getJSONObject(0);
                payViaIPay88(argObj);
            }
            
            return true;
        } else if(action.equals("requery")){       
            cordovaCallbackContext = callbackContext;
            Requery(argObj);            
            return true;
        }else {
            return false;
        }
    }

    private void initCanceledResult ()
    {
        r_status = ResultDelegate.STATUS_CANCELED;
        r_transactionId = "";
        r_referenceNo = "";
        r_amount = "";
        r_remarks = "";
        r_authCode = "";
        r_err = "cancelled";
    }

    private void payViaIPay88 (JSONObject argObj) throws JSONException
    {
        int amount;
        String name, email, phone, refNo, currency, country,
               description, remark, paymentId, lang, merchantKey, merchantCode,
               backendPostUrl;

        try {
            amount = argObj.getInt("amount");
            name = argObj.getString("name");
            email = argObj.getString("email");
            phone = argObj.getString("phone");
            refNo = argObj.getString("refNo");
            currency = argObj.getString("currency");
            country = argObj.getString("country");
            description = argObj.getString("description");
            remark = argObj.getString("remark");
            paymentId = argObj.getString("paymentId");
            lang = argObj.getString("lang");
            merchantKey = argObj.getString("merchantKey");
            merchantCode = argObj.getString("merchantCode");
            backendPostUrl = argObj.getString("backendPostUrl");
            // Log.i("### payViaIPay88 ", "try ended");
        } catch (Exception e) {
            cordovaCallbackContext.error("Required parameter missing or invalid. "+e.getMessage());
            return;
        }

        try {
	        // iPay object.
	        IPayIHPayment payment = new IPayIHPayment();
	        payment.setMerchantKey(merchantKey);
	        payment.setMerchantCode(merchantCode);
	        //payment.setAmount(String.format(Locale.US, "%.2f", Integer.valueOf(amount).floatValue()/100.0)); // http://developer.android.com/reference/java/util/Locale.html#default_locale
	        payment.setAmount(String.format(Locale.US, "%.2f", Integer.valueOf(amount).floatValue()/1.0)); // http://developer.android.com/reference/java/util/Locale.html#default_locale
	        payment.setUserName(name);
	        payment.setUserEmail(email);
	        payment.setUserContact(phone);
	        payment.setRefNo(refNo);
	        payment.setCurrency(currency);
	        payment.setCountry(country);
	        payment.setProdDesc(description);
	        payment.setRemark(remark);
	        payment.setPaymentId(paymentId);
	        payment.setLang(lang);
	        payment.setBackendPostURL(backendPostUrl);
	        
	        // Create and save the iPay88 results delegate.
	        iPayDelegate = new ResultDelegate(); 

	        // iPay88 intent.
	        Intent checkoutIntent = IPayIH.getInstance().checkout(
	            payment,
	            cordova.getActivity(),
	            iPayDelegate, 
	            IPayIH.PAY_METHOD_CREDIT_CARD
	        );

	        // Launch the iPay88 activity.
	        // 1st arg to startActivityForResult() must be null, otherwise all WebViews get pause
	        // leading to stuck iPay88 activity??
	        isInProgress = true;
	        Log.i("### Execute Cordova ", cordova.toString());
	        Log.i("### Execute checkoutIntent ", checkoutIntent.toString());
	        cordova.startActivityForResult(null, checkoutIntent, IPAY88_ACT_REQUEST_CODE);
	        cordova.setActivityResultCallback(this);
	    } catch (Exception e) {
	    	Log.i("### Execute exp ", e.toString());
            e.printStackTrace();
        }
	}	
    
    private void Requery(JSONObject argObj) throws JSONException
    {
        int amount;
        String refNo, merchantCode;
        
        //Log.e(TAG,argObj.toString());
        
        try {
            amount = argObj.getInt("amount");
            refNo = argObj.getString("refNo");
            merchantCode = argObj.getString("merchantCode");            
            
        } catch (Exception e) {
            cordovaCallbackContext.error("Required parameter missing or invalid. "+e.getMessage());
            //Log.e(TAG,e.getMessage());
            return;
        }
        
        IPayIHR r = new IPayIHR();
        r.setMerchantCode(merchantCode);
        r.setRefNo(refNo);
        r.setAmount(String.format(Locale.US, "%.2f", Integer.valueOf(amount).floatValue()/1.0));
        
        //Log.e(TAG,r.getMerchantCode());
        //Log.e(TAG,r.getAmount());
        //Log.e(TAG,r.getRefNo());
        
        
        try{
            // Create and save the iPay88 results delegate.
            iPayDelegate = new ResultDelegate();

            Intent checkoutIntent = IPayIH.getInstance().requery(
            	r, 
            	cordova.getActivity(),
            	iPayDelegate
	        );
            
            cordova.startActivityForResult(null, checkoutIntent, IPAY88_ACT_REQUEST_CODE);
            cordova.setActivityResultCallback(this);
        }catch (Exception e){
            cordovaCallbackContext.error("Intent Error " +e.getMessage());
        }               
        
    }
}
