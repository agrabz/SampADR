package com.sap.akos.samp.adr.app;

import android.content.Intent;
import com.sap.cloud.mobile.flowv2.ext.FlowStateListener;
import com.sap.cloud.mobile.foundation.model.AppConfig;
import android.content.SharedPreferences;
import com.sap.cloud.mobile.foundation.authentication.AppLifecycleCallbackHandler;
import com.sap.cloud.mobile.foundation.networking.HttpException;
import com.sap.cloud.mobile.foundation.settings.policies.ClientPolicies;
import com.sap.cloud.mobile.foundation.settings.policies.LogPolicy;
import ch.qos.logback.classic.Level;
import android.widget.Toast;
import java.util.HashMap;
import java.util.Map;
import com.sap.akos.samp.adr.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sap.cloud.mobile.flowv2.ext.ConsentType;
import kotlin.Pair;
import java.util.List;

import com.sap.akos.samp.adr.fcm.NotificationUtilities;
import com.sap.cloud.mobile.foundation.usage.AppUsageUploader;
import com.sap.cloud.mobile.foundation.usage.UsageBroker;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WizardFlowStateListener extends FlowStateListener {
    private static Logger logger = LoggerFactory.getLogger(WizardFlowStateListener.class);
    public static final String USAGE_SERVICE_PRE = "pref_usage_service";
    public static final String CRASH_SERVICE_PRE = "pref_crash_service";
    private final SAPWizardApplication application;
    private static boolean isUsageEnabled;
    private static int uploadInterval;

    public WizardFlowStateListener(@NotNull SAPWizardApplication application) {
        super();
        this.application = application;
    }

    @Override
    public void onAppConfigRetrieved(@NotNull AppConfig appConfig) {
        logger.debug(String.format("onAppConfigRetrieved: %s", appConfig.toString()));
        application.initializeServiceManager(appConfig);
        application.setAppConfig(appConfig);
    }

    @Override
    public void onApplicationReset() {
        this.application.resetApp();
        Intent intent = new Intent(application, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        application.startActivity(intent);
    }

    @Override
    public void onApplicationLocked() {
        super.onApplicationLocked();
        application.isApplicationUnlocked = false;
    }

    @Override
    public void onFlowFinished(@Nullable String flowName) {
        if(flowName != null) {
            application.isApplicationUnlocked = true;
        }
        if(application.notificationMessage != null) {
            NotificationUtilities.showNotificationMessage(application.notificationMessage);
        }
    }

    @Override
    public void onClientPolicyRetrieved(@NotNull ClientPolicies policies) {
        SharedPreferences sp = application.sp;
        String logString = sp.getString(SAPWizardApplication.KEY_LOG_SETTING_PREFERENCE, "");
        LogPolicy currentSettings;
        if (logString.isEmpty()) {
            currentSettings = new LogPolicy();
        } else {
            currentSettings = LogPolicy.createFromJsonString(logString);
        }

        LogPolicy logSettings = policies.getLogPolicy();
        if (!currentSettings.getLogLevel().equals(logSettings.getLogLevel()) || logString.isEmpty()) {
            sp.edit().putString(SAPWizardApplication.KEY_LOG_SETTING_PREFERENCE,
                    logSettings.toString()).apply();
            LogPolicy.setRootLogLevel(logSettings);
            AppLifecycleCallbackHandler.getInstance().getActivity().runOnUiThread(() -> {
                Map mapping = new HashMap<Level, String>();
                mapping.put(Level.ALL, application.getString(R.string.log_level_path));
                mapping.put(Level.DEBUG, application.getString(R.string.log_level_debug));
                mapping.put(Level.INFO, application.getString(R.string.log_level_info));
                mapping.put(Level.WARN, application.getString(R.string.log_level_warning));
                mapping.put(Level.ERROR, application.getString(R.string.log_level_error));
                mapping.put(Level.OFF, application.getString(R.string.log_level_none));
                Toast.makeText(
                        application,
                        String.format(
                                application.getString(R.string.log_level_changed),
                                mapping.get(LogPolicy.getLogLevel(logSettings))
                        ),
                        Toast.LENGTH_SHORT
                ).show();
                logger.info(String.format(
                                application.getString(R.string.log_level_changed),
                                mapping.get(LogPolicy.getLogLevel(logSettings))
                        ));
            });
        }
        if (policies.getUsagePolicy() != null) {
            isUsageEnabled = policies.getUsagePolicy().getDataCollectionEnabled();
            uploadInterval = policies.getUsagePolicy().getUploadDataAfterDays();
            if (isUsageEnabled) {
                UsageBroker.setDataCollectionEnabled(isUsageEnabled);
                uploadUsage();
            }
        }
    }
    private void uploadUsage() {
        UsageBroker.setDaysToWaitBetweenUpload(uploadInterval);

        //if uploadInterval is greater than 0 then auto-upload is considered to be enabled on Mobile Services
        if (uploadInterval > 0) {
            // The upload will only occur if the last upload was more than newDays ago
            AppUsageUploader.addUploadListener(new AppUsageUploader.UploadListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(application,
                            application.getString(R.string.usage_upload_ok),
                            Toast.LENGTH_LONG).show();
                }

                @Override
                public void onError(Throwable error) {
                    if (error instanceof HttpException) {
                        logger.debug("Usage Upload server error: {}, code = {}",
                                ((HttpException) error).message(), ((HttpException) error).code());
                    } else {
                        logger.debug("Usage Upload error: {}", error.getMessage());
                    }
                    String errorMessage = application.getString(R.string.usage_upload_failed);
                    logger.error(errorMessage, error);
                }

                @Override
                public void onProgress(int i) {
                    logger.debug("Usage upload progress: " + i);
                }
            });
            UsageBroker.upload(application, false);
        }
    }

    @Override
    public void onConsentStatusChange(
            @NotNull List<? extends Pair<? extends ConsentType, Boolean>> consents) {
        SharedPreferences sp = application.sp;
        for( Pair<? extends ConsentType, Boolean> consent: consents ) {
            ConsentType first = consent.getFirst();
            if (first == ConsentType.USAGE) {
                sp.edit().putBoolean(USAGE_SERVICE_PRE, consent.getSecond()).apply();
            } else if (first == ConsentType.CRASH_REPORT) {
                sp.edit().putBoolean(CRASH_SERVICE_PRE, consent.getSecond()).apply();
            }
        }
    }

}
