package com.mct.app.helper.admob.ads.natives;

import com.mct.app.helper.R;

public enum NativeTemplate {

    SMALL(R.layout.gnt_small_template_view),
    MEDIUM(R.layout.gnt_medium_template_view),
    LARGE(R.layout.gnt_large_template_view);

    public final int layoutRes;

    NativeTemplate(int layoutRes) {
        this.layoutRes = layoutRes;
    }
}
