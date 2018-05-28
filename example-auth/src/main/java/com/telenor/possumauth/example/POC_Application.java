package com.telenor.possumauth.example;

import android.app.Application;
import android.content.Context;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.MailSenderConfigurationBuilder;

@AcraCore(buildConfigClass = BuildConfig.class)
public class POC_Application extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        CoreConfigurationBuilder builder = new CoreConfigurationBuilder(base);
        builder.getPluginConfigurationBuilder(MailSenderConfigurationBuilder.class).setEnabled(true).setMailTo(getString(R.string.acra_mail));
        // The following line triggers the initialization of ACRA
        ACRA.init(this, builder);
    }
}