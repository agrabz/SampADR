package com.sap.akos.samp.adr.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.sap.akos.samp.adr.R;
import com.sap.cloud.mobile.fiori.onboarding.LaunchScreen;
import com.sap.cloud.mobile.fiori.onboarding.ext.LaunchScreenSettings;
import com.sap.cloud.mobile.flowv2.core.DialogHelper;
import com.sap.cloud.mobile.flowv2.core.Flow;
import com.sap.cloud.mobile.foundation.model.AppConfig;
import com.sap.cloud.mobile.flowv2.model.FlowConstants;
import com.sap.cloud.mobile.flowv2.core.FlowContext;
import com.sap.cloud.mobile.flowv2.core.FlowContextBuilder;


import com.sap.cloud.mobile.foundation.configurationprovider.ConfigurationLoader;
import com.sap.cloud.mobile.foundation.configurationprovider.ConfigurationLoaderCallback;
import com.sap.cloud.mobile.foundation.configurationprovider.ConfigurationPersistenceException;
import com.sap.cloud.mobile.foundation.configurationprovider.ConfigurationProvider;
import com.sap.cloud.mobile.foundation.configurationprovider.ConfigurationProviderError;
import com.sap.cloud.mobile.foundation.configurationprovider.DefaultPersistenceMethod;
import com.sap.cloud.mobile.foundation.configurationprovider.FileConfigurationProvider;
import com.sap.cloud.mobile.foundation.configurationprovider.ProviderIdentifier;
import com.sap.cloud.mobile.foundation.configurationprovider.UserInputs;

import com.sap.cloud.mobile.foundation.remotenotification.PushRemoteMessage;
import com.sap.akos.samp.adr.fcm.NotificationUtilities;

import org.json.JSONObject;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class WelcomeActivity extends AppCompatActivity {
    private boolean isFlowStarted = false;
    private FragmentManager fManager = this.getSupportFragmentManager();
    private PushRemoteMessage message;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getStringExtra("alert") != null) {
            message = new PushRemoteMessage();
            message.setAlert(getIntent().getStringExtra("alert"));
        }
        if (message == null) {
            message = getIntent().getParcelableExtra(NotificationUtilities.NOTIFICATION_DATA);
        }

        if (message != null) {
            ((SAPWizardApplication) getApplication()).notificationMessage = message;
        }
        LaunchScreen welcome = new LaunchScreen(this);
        welcome.initialize(new LaunchScreenSettings.Builder()
                .setDemoButtonVisible(false)
                .setHeaderLineLabel(getString(R.string.welcome_screen_headline_label))
                .setPrimaryButtonText(getString(R.string.welcome_screen_primary_button_label))
                .setFooterVisible(false)
//                .setUrlTermsOfService("http://www.sap.com")
//                .setUrlPrivacy("http://www.sap.com")
                .addInfoViewSettings(
                        new LaunchScreenSettings.LaunchScreenInfoViewSettings(
                                R.drawable.ic_baseline_sentiment_satisfied_alt_24,
                                getString(R.string.application_display_name),
                                getString(R.string.welcome_screen_custom_label)
                        )
                )
                .build());
        welcome.setPrimaryButtonOnClickListener(v -> {
            if (!isFlowStarted) {
                startConfigurationLoader();
            }
        });
        setContentView(welcome);
    }

    private void startConfigurationLoader(){
        Activity activity = this;
        ConfigurationLoaderCallback callback = new ConfigurationLoaderCallback() {
            @Override
            public void onCompletion(@Nullable ProviderIdentifier providerIdentifier, boolean success) {
                if (success) {
                    startFlow(activity);
                } else {
                    new DialogHelper(getApplication(), R.style.OnboardingDefaultTheme_Dialog_Alert)
                            .showOKOnlyDialog(
                                    fManager,
                                    getResources().getString(R.string.config_loader_complete_error_description),
                                    null, null, null
                            );
                }
            }

            @Override
            public void onError(@NonNull ConfigurationLoader configurationLoader, @NonNull ProviderIdentifier providerIdentifier, @NonNull UserInputs userInputs, @NonNull ConfigurationProviderError configurationProviderError) {
                new DialogHelper(getApplication(), R.style.OnboardingDefaultTheme_Dialog_Alert)
                        .showOKOnlyDialog(
                                fManager,
                                String.format(getResources().getString(
                                        R.string.config_loader_on_error_description),
                                        providerIdentifier.toString(), configurationProviderError.getErrorMessage()
                                ),
                                null, null, null
                        );
                configurationLoader.processRequestedInputs(new UserInputs());
            }

            @Override
            public void onInputRequired(@NonNull ConfigurationLoader configurationLoader, @NonNull UserInputs userInputs) {
                configurationLoader.processRequestedInputs(new UserInputs());
            }
        };
        ConfigurationProvider[] providers = {new FileConfigurationProvider(this, "sap_mobile_services")};
        this.runOnUiThread(() -> {
            ConfigurationLoader loader = new ConfigurationLoader(this, callback, providers);
            loader.loadConfiguration();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FlowConstants.FLOW_ACTIVITY_REQUEST_CODE) {
            isFlowStarted = false;
            if (resultCode == Activity.RESULT_OK) {
                Intent intent = new Intent(this, MainBusinessActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }
    }

    public void startFlow(Activity context) {
        AppConfig appConfig = prepareAppConfig();
        if (appConfig == null) {
            return;
        }

        FlowContext flowContext = new FlowContextBuilder()
                .setApplication(appConfig)
                .setMultipleUserMode(true)
                .setFlowStateListener(new WizardFlowStateListener(
                        (SAPWizardApplication) context.getApplication()))
                .build();

        Flow.start(context, flowContext);
        isFlowStarted = true;
    }

    private AppConfig prepareAppConfig() {
        try {
            JSONObject configData = DefaultPersistenceMethod.getPersistedConfiguration(this);
            AppConfig config = AppConfig.createAppConfigFromJsonString(configData.toString());

            return config;
        } catch (ConfigurationPersistenceException ex) {
            new DialogHelper(this, R.style.OnboardingDefaultTheme_Dialog_Alert)
                    .showOKOnlyDialog(
                            fManager,
                            getResources().getString(R.string.config_data_build_json_description),
                            null, null, null
                    );
            return null;
        } catch (Exception ex) {
            new DialogHelper(this, R.style.OnboardingDefaultTheme_Dialog_Alert)
                    .showOKOnlyDialog(
                            fManager,
                            ex.getLocalizedMessage(),
                            null, null, null
                    );
            return null;
        }
    }
}