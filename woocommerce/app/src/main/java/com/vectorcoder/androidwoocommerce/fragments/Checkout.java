package com.vectorcoder.androidwoocommerce.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.gson.Gson;
import com.vectorcoder.androidwoocommerce.R;

import com.vectorcoder.androidwoocommerce.activities.MainActivity;
import com.vectorcoder.androidwoocommerce.app.App;
import com.vectorcoder.androidwoocommerce.constant.ConstantValues;
import com.vectorcoder.androidwoocommerce.customs.DialogLoader;
import com.vectorcoder.androidwoocommerce.models.order_model.OrderDetails;
import com.vectorcoder.androidwoocommerce.models.order_model.PostOrder;
import com.vectorcoder.androidwoocommerce.utils.NotificationHelper;
import com.vectorcoder.androidwoocommerce.oauth.OAuthEncoder;


public class Checkout extends Fragment {
    
    View rootView;
    
    WebView checkout_webView;
    
    PostOrder postOrder;
    OrderDetails orderDetails;
    DialogLoader dialogLoader;
    
    String ORDER_RECEIVED = "order-received";
    String CHECKOUT_URL = ConstantValues.WOOCOMMERCE_URL+"android-mobile-checkout";
    
    
    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.checkout, container, false);

        // Set the Title of Toolbar
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(getString(R.string.checkout));

        // Get the User's Info from the AppContext
        orderDetails = ((App) getContext().getApplicationContext()).getOrderDetails();
        

        // Binding Layout Views
        checkout_webView = (WebView) rootView.findViewById(R.id.checkout_webView);
    
    
        postOrder = new PostOrder();
    
        postOrder.setOrderProducts(orderDetails.getOrderProducts());
        postOrder.setOrderCoupons(orderDetails.getOrderCoupons());
        
        if (!ConstantValues.IS_ONE_PAGE_CHECKOUT_ENABLED) {
            postOrder.setBilling(orderDetails.getBilling());
            postOrder.setShipping(orderDetails.getShipping());
            postOrder.setSameAddress(orderDetails.isSameAddress());
            postOrder.setOrderShippingMethods(orderDetails.getOrderShippingMethods());
        }
    
        if (!ConstantValues.IS_GUEST_LOGGED_IN) {
            postOrder.setToken(orderDetails.getToken());
            postOrder.setCustomerId(orderDetails.getCustomerId());
        }
    
        dialogLoader = new DialogLoader(getContext());
        
        
        Gson gson = new Gson();
    
        // 2. Java object to JSON, and assign to a String
        String jsonData = gson.toJson(postOrder);
    
        String encodedData = OAuthEncoder.encode(jsonData);
        String url = CHECKOUT_URL+"?order="+encodedData;
    
        
        Log.i("VC_Shop", "customer_ID= "+orderDetails.getCustomerId());
        Log.i("VC_Shop", "customer_Token= "+orderDetails.getToken());
        Log.i("VC_Shop", "order_url= "+CHECKOUT_URL);
        Log.i("VC_Shop", "order_url_params= "+jsonData);
        Log.i("VC_Shop", "url= "+url);
        
        
        checkout_webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
                Log.i("order", "onPageStarted: url="+url);
            }
            
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                dialogLoader.showProgressDialog();
                Log.i("VC_Shop", "onPageStarted: url= "+url);
                
                if (url.contains(ORDER_RECEIVED)) {
                    view.stopLoading();
    
                    Intent notificationIntent = new Intent(getContext(), MainActivity.class);
                    notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    
                    // Order has been placed Successfully
                    NotificationHelper.showNewNotification(getContext(), notificationIntent, getString(R.string.thank_you), getString(R.string.order_placed));
                    
                    // Clear User's Cart
                    My_Cart.ClearCart();
    
                    // Navigate to Thank_You Fragment
                    Fragment fragment = new Thank_You();
                    FragmentManager fragmentManager = getFragmentManager();
                    fragmentManager.popBackStack(getString(R.string.actionCart), FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    fragmentManager.beginTransaction()
                            .replace(R.id.main_fragment, fragment)
                            .commit();
                }
            }
    
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                dialogLoader.hideProgressDialog();
                Log.i("VC_Shop", "onPageFinished: url= "+url);
            }
    
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                dialogLoader.hideProgressDialog();
                Log.i("VC_Shop", "onReceivedError: error= "+error);
            }
        });
    
        checkout_webView.setVerticalScrollBarEnabled(false);
        checkout_webView.setHorizontalScrollBarEnabled(false);
        checkout_webView.setBackgroundColor(Color.TRANSPARENT);
        checkout_webView.getSettings().setJavaScriptEnabled(true);
        checkout_webView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        checkout_webView.loadUrl(url);
        

        return rootView;
    }

}



