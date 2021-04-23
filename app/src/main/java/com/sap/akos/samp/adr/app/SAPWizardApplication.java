package com.sap.akos.samp.adr.app;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import com.sap.cloud.mobile.foundation.authentication.AppLifecycleCallbackHandler;
import com.sap.cloud.mobile.foundation.model.AppConfig;
import com.sap.akos.samp.adr.service.SAPServiceManager;
import com.sap.akos.samp.adr.repository.RepositoryFactory;
import com.sap.cloud.mobile.foundation.mobileservices.MobileService;
import com.sap.cloud.mobile.foundation.mobileservices.SDKInitializer;
import java.util.ArrayList;
import java.util.List;
import androidx.annotation.NonNull;
import com.sap.akos.samp.adr.fcm.FCMPushCallbackListenner;
import com.sap.cloud.mobile.foundation.remotenotification.RemoteNotificationClient;
import com.sap.cloud.mobile.foundation.remotenotification.PushRemoteMessage;
import com.sap.cloud.mobile.foundation.remotenotification.PushService;
import com.sap.cloud.mobile.foundation.logging.Logging;
import com.sap.cloud.mobile.foundation.logging.LogService;
import ch.qos.logback.classic.Level;
import com.sap.cloud.mobile.foundation.usage.UsageService;
import com.sap.cloud.mobile.foundation.crash.CrashService;

public class SAPWizardApplication extends Application {

    /**
     * The application configuration information.
     */
    private AppConfig appConfig;
    public boolean isApplicationUnlocked = false;
    public static SharedPreferences sp;

    public static final String KEY_LOG_SETTING_PREFERENCE = "key.log.settings.preference";

    /**
     * Manages and provides access to OData stores providing data for the app.
     */
    private SAPServiceManager sapServiceManager;
    /**
     * Application-wide RepositoryFactory
     */
    private RepositoryFactory repositoryFactory;
    public PushRemoteMessage notificationMessage;
    /**
     * Returns the application-wide service manager.
     *
     * @return the service manager
     */
    public SAPServiceManager getSAPServiceManager() {
        return sapServiceManager;
    }
    /**
     * Returns the application-wide repository factory
     *
     * @return the repository factory
     */
    public RepositoryFactory getRepositoryFactory() {
        return repositoryFactory;
    }

    /**
     * Clears all user-specific data from the application, essentially resetting
     * it to its initial state.
     *
     * If client code wants to handle the reset logic of a service, here is an example:
     * 
     *     SDKInitializer.INSTANCE.resetServices( service -> {
     *             if(service instanceof PushService) {
     *                 PushService.unregisterPushSync(new RemoteNotificationClient.CallbackListener() {
     *                     @Override
     *                     public void onSuccess() {
     *
     *                     }
     *
     *                     @Override
     *                     public void onError(@NonNull Throwable throwable) {
     *
     *                     }
     *                 });
     *                 return true;
     *             } else {
     *                 return false;
     *             }
     *         });
     */
    public void resetApp() {
        sp.edit().clear().apply();
        appConfig = null;
        isApplicationUnlocked = false;
        repositoryFactory.reset();
        SDKInitializer.INSTANCE.resetServices(null);
        notificationMessage = null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        registerActivityLifecycleCallbacks(AppLifecycleCallbackHandler.getInstance());
        initService();
        clearConsentStatus();

    }

    private void clearConsentStatus() {
        sp.edit().putBoolean(WizardFlowStateListener.USAGE_SERVICE_PRE, false).apply();
            sp.edit().putBoolean(WizardFlowStateListener.CRASH_SERVICE_PRE, false).apply();

    }
    private void initService() {
        List<MobileService> services = new ArrayList<>();
        services.add(new UsageService());
        services.add(new CrashService(true));
        PushService pushService = new PushService();
        pushService.setPushCallbackListener(new FCMPushCallbackListenner());
        services.add(pushService);
        Logging.setConfigurationBuilder(new Logging.ConfigurationBuilder().initialLevel(Level.WARN).logToConsole(true).build());
        services.add(new LogService());


        SDKInitializer.INSTANCE.start(this, services.toArray(new MobileService[0]), null);
    }

    /**
     * Initialize service manager with application configuration
     *
     * @param appConfig the application configuration
     */
    public void initializeServiceManager(AppConfig appConfig) {
        sapServiceManager = new SAPServiceManager(appConfig);
        repositoryFactory = new RepositoryFactory(sapServiceManager);
    }

    public AppConfig getAppConfig() {
        return appConfig;
    }

    public void setAppConfig(AppConfig appConfig) {
        this.appConfig = appConfig;
    }
}
