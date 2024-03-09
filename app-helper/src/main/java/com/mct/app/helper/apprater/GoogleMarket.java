package com.mct.app.helper.apprater;

import android.content.Context;
import android.net.Uri;

public class GoogleMarket extends Market {

    private static final String marketLink = "market://details?id=";

    @Override
    public Uri getMarketURI(Context context) {
        return Uri.parse(marketLink + getPackageName(context));
    }
}