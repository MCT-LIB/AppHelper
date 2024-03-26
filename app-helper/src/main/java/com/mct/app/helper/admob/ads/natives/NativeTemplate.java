package com.mct.app.helper.admob.ads.natives;

import com.mct.app.helper.R;

public enum NativeTemplate {

    SMALL(R.layout.gnt_template_view_small),
    MEDIUM(R.layout.gnt_template_view_medium),
    LARGE(R.layout.gnt_template_view_large),
    A4_PAGE(R.layout.gnt_template_view_a4);

    public final int layoutRes;

    NativeTemplate(int layoutRes) {
        this.layoutRes = layoutRes;
    }
}
