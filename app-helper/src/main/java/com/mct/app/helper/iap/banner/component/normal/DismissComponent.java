package com.mct.app.helper.iap.banner.component.normal;

import android.view.View;

import androidx.annotation.NonNull;

import com.mct.app.helper.iap.banner.IapBanner;

/**
 * DismissComponent - A component for dismissing an IapBanner.
 * <p>
 * The DismissComponent class allows you to create a component within an IapBanner that, when
 * clicked, dismisses the banner. You can also set an optional click listener to perform
 * additional actions when the component is clicked.
 */
public class DismissComponent<C extends DismissComponent<C>> extends Component<C> {


    /**
     * {@inheritDoc}
     */
    public DismissComponent(int id) {
        super(id);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void init(@NonNull IapBanner banner, View root) {
        super.init(banner, root);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void release(@NonNull IapBanner banner, View root) {
        super.release(banner, root);
    }

    /**
     * Setup an OnClickListener for the DismissComponent's view, dismissing the IapBanner when
     * clicked.
     *
     * @param banner The IapBanner instance to which the component belongs.
     * @param root   The root View of the IapBanner layout.
     */
    @Override
    protected void setupOnClickListener(@NonNull IapBanner banner, View root) {
        if (view != null) {
            view.setOnClickListener(v -> {
                banner.dismiss();
                if (clickListener != null) {
                    clickListener.onClick(v);
                }
            });
        }
    }
}
