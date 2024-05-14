package com.mct.app.helper.admob.ads.natives;

import com.mct.app.helper.R;

public enum NativeTemplate {

    EXTRA_LARGE(R.layout.gnt_template_view_extra_large),
    LARGE_1(R.layout.gnt_template_view_large_1),
    LARGE_2(R.layout.gnt_template_view_large_2),
    MEDIUM_1(R.layout.gnt_template_view_medium_1),
    MEDIUM_2(R.layout.gnt_template_view_medium_2),
    SMALL(R.layout.gnt_template_view_small),
    SMALL_A4(R.layout.gnt_template_view_small_a4),
    SMALL_RECT(R.layout.gnt_template_view_small_rect),
    SMALL_SQUARE(R.layout.gnt_template_view_small_square);

    public final int layoutRes;

    NativeTemplate(int layoutRes) {
        this.layoutRes = layoutRes;
    }
}
