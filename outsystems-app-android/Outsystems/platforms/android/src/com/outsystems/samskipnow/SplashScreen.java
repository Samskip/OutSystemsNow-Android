/*
 * OutSystems Project
 * 
 * Copyright (C) 2014 OutSystems.
 * 
 * This software is proprietary.
 */
package com.outsystems.samskipnow;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.arellomobile.android.push.PushManager;
import com.outsystems.samskipnow.core.DatabaseHandler;
import com.outsystems.samskipnow.core.EventLogger;
import com.outsystems.samskipnow.helpers.DeepLinkController;
import com.outsystems.samskipnow.helpers.HubManagerHelper;
import com.outsystems.samskipnow.helpers.OfflineSupport;
import com.outsystems.samskipnow.model.DeepLink;
import com.outsystems.samskipnow.model.HubApplicationModel;

/**
 * Class description.
 * 
 * @author <a href="mailto:vmfo@xpand-it.com">vmfo</a>
 * @version $Revision: 666 $
 * 
 */
public class SplashScreen extends Activity {

    /** The time splash screen. */
    public static int TIME_SPLASH_SCREEN = 2000;
    private static PushManager pushManager;

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splashscreen);

        // Removing session cookies
        CookieSyncManager.createInstance(getApplicationContext());
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeSessionCookie();
        CookieSyncManager.getInstance().sync();

        // Push Messages    	
        try {
            // Create and start push manager
            pushManager = PushManager.getInstance(this);

            pushManager.onStartup(this);

            // Register for push!
            pushManager.registerForPushNotifications();
        } catch (Exception e) {
            // push notifications are not available or AndroidManifest.xml is not configured properly
            EventLogger.logError(getClass(), e);
        }
  
        // Get data from Deep Link
        Uri data = this.getIntent().getData();

        if(data != null){
        	DeepLinkController.getInstance().createSettingsFromUrl(data);
        }        
        
        
        // Add delay to show splashscreen
        delaySplashScreen();
    }

    private void delaySplashScreen() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                goNextActivity();
            }
        }, TIME_SPLASH_SCREEN);
    }

    protected void goNextActivity() {

        ApplicationOutsystems app = (ApplicationOutsystems)getApplication();

        // Working Offline
        if(!app.isNetworkAvailable()) {
            // Check if the last credentials were valid
            if(OfflineSupport.getInstance(getApplicationContext()).hasValidCredentials()){
               OfflineSupport.getInstance(getApplicationContext()).redirectToApplicationList(this);

               // Finish activity
               finish();
               return;
            }
        }

        DatabaseHandler database = new DatabaseHandler(getApplicationContext());
        List<HubApplicationModel> hubApplications = database.getAllHubApllications();

        if(database != null) {

            HubApplicationModel last = database.getLastLoginHubApplicationModel();

            String lastUser = "null";

            if(last != null) {
                lastUser = last.getUserName() + " - " + last.getDateLastLogin();
            }

            EventLogger.logInfoMessage(this.getClass(), "Last:" + lastUser);

        }

        if(DeepLinkController.getInstance().hasValidSettings()){
        	DeepLink deepLinkSettings = DeepLinkController.getInstance().getDeepLinkSettings();
        	HubManagerHelper.getInstance().setApplicationHosted(deepLinkSettings.getEnvironment());

            Intent intent = new Intent(getApplicationContext(), HubAppActivity.class);
            startActivity(intent);

        }
        else{
            openHubActivity();
            
	        if (hubApplications != null && hubApplications.size() > 0) {
	            HubApplicationModel hubApplication = hubApplications.get(0);
	            if (hubApplication != null) {
	                HubManagerHelper.getInstance().setApplicationHosted(hubApplication.getHost());
	                HubManagerHelper.getInstance().setJSFApplicationServer(hubApplication.isJSF());
	            }
	            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
	            if (hubApplication != null) {
	                intent.putExtra(LoginActivity.KEY_INFRASTRUCTURE_NAME, hubApplication.getName());
	                intent.putExtra(LoginActivity.KEY_AUTOMATICLY_LOGIN, true);
	            }
	            startActivity(intent);
	        }
	    }
        finish();
    }

    private void openHubActivity() {
        Intent intent = new Intent(this, HubAppActivity.class);
        startActivity(intent);
    }        
          
}
