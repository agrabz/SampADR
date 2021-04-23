package com.sap.akos.samp.adr.app;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import com.sap.akos.samp.adr.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sap.akos.samp.adr.mdui.EntitySetListActivity;
import com.sap.akos.samp.adr.fcm.NotificationUtilities;
import com.sap.akos.samp.adr.mdui.productcategories.ProductCategoriesActivity;

public class MainBusinessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_business);
    }

    private void startEntitySetListActivity() {
        SAPWizardApplication application = (SAPWizardApplication) getApplication();
        application.getSAPServiceManager().openODataStore(() -> {
            Intent intent = new Intent(this, EntitySetListActivity.class);
            Intent pcIntent = new Intent(this, ProductCategoriesActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            startActivity(pcIntent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        SAPWizardApplication application = (SAPWizardApplication) getApplication();
        if( application.notificationMessage != null ) {
            NotificationUtilities.showMessageDialog(this, application.notificationMessage, () -> {
                startEntitySetListActivity();
            });
        } else {
            startEntitySetListActivity();
        }
    }

}
