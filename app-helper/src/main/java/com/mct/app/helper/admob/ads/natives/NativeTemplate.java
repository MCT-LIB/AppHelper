package com.mct.app.helper.admob.ads.natives;

import com.mct.app.helper.R;

public enum NativeTemplate {

    SMALL(R.layout.gnt_template_view_small),
    MEDIUM(R.layout.gnt_template_view_medium),
    LARGE(R.layout.gnt_template_view_large),
    SMALL_A4(R.layout.gnt_template_view_small_a4),
    SMALL_RECT(R.layout.gnt_template_view_small_rect),
    SMALL_SQUARE(R.layout.gnt_template_view_small_square);

    public final int layoutRes;

    NativeTemplate(int layoutRes) {
        this.layoutRes = layoutRes;
    }
}
