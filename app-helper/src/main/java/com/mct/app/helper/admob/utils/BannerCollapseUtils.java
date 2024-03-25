package com.mct.app.helper.admob.utils;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

public class BannerCollapseUtils {

    public static boolean hasBannerCollapsePopup() {
        return !getCollapsePopupWindowViews().isEmpty();
    }

    public static void dismissBannerCollapsePopups() {
        MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_OUTSIDE, 0, 0, 0);
        event.recycle();
        for (View view : getCollapsePopupWindowViews()) {
            view.onTouchEvent(event);
        }
    }

    public static List<View> getCollapsePopupWindowViews() {
        return getWindowViews().stream()
                .filter(BannerCollapseUtils::isPopupWindow)
                .filter(BannerCollapseUtils::isContainAdsView)
                .collect(Collectors.toList());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private area
    ///////////////////////////////////////////////////////////////////////////

    private static Object sWindowManagerGlobal;
    private static Field sWindowManagerGlobalViewsField;

    static {
        try {
            @SuppressLint("PrivateApi")
            Class<?> clazz = Class.forName("android.view.WindowManagerGlobal");
            sWindowManagerGlobal = clazz.getMethod("getInstance").invoke(null);
            sWindowManagerGlobalViewsField = clazz.getDeclaredField("mViews");
            sWindowManagerGlobalViewsField.setAccessible(true);
        } catch (Exception ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private static List<View> getWindowViews() {
        try {
            return (List<View>) sWindowManagerGlobalViewsField.get(sWindowManagerGlobal);
        } catch (Throwable ignored) {
            return Collections.emptyList();
        }
    }

    private static boolean isPopupWindow(View view) {
        Class<?>[] classes = PopupWindow.class.getDeclaredClasses();
        for (Class<?> clazz : classes) {
            // correct class name -> just check if it is an instance
            if (clazz.getSimpleName().equals("PopupDecorView")) {
                return clazz.isInstance(view);
            }
            // try to match the class name
            if (clazz.isInstance(view)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isContainAdsView(View view) {
        Deque<ViewGroup> stack = new ArrayDeque<>();
        if (view instanceof ViewGroup) {
            stack.push((ViewGroup) view);
        }
        while (!stack.isEmpty()) {
            ViewGroup currentViewGroup = stack.pop();
            int childCount = currentViewGroup.getChildCount();

            for (int i = 0; i < childCount; i++) {
                View child = currentViewGroup.getChildAt(i);
                if (child.getClass().getName().startsWith("com.google.android.gms.ads")) {
                    return true;
                }
                if (child instanceof ViewGroup) {
                    stack.push((ViewGroup) child);
                }
            }
        }
        return false;
    }

    private BannerCollapseUtils() {
        //no instance
    }
}
