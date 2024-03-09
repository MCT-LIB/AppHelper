package com.mct.app.helper.apprater;

import android.content.Context;
import android.net.Uri;

public class AmazonMarket extends Market {

    private static final String marketLink = "http://www.amazon.com/gp/mas/dl/android?p=";

    @Override
    public Uri getMarketURI(Context context) {
        return Uri.parse(marketLink + getPackageName(context));
    }
}