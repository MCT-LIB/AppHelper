package com.mct.app.helper.iap.banner.component.normal;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import com.mct.app.helper.iap.banner.IapBanner;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * TextComponent - A component for displaying styled text within an IapBanner.
 * <p>
 * The TextComponent class allows you to display styled text within an IapBanner. You can customize
 * the text style, such as bold, italic, strikethrough, underline, and text highlighting.
 * <p>
 * Usage example:
 * <code>
 * <pre>
 *
 * // Create a TextComponent with custom text and style
 * TextComponent<?> textComponent = new TextComponent<>(R.id.text_component_id)
 *         .text("This is a sample text")
 *         .bold()
 *         .italic()
 *         .underline()
 *         .highlight(Color.RED);
 * </pre>
 * </code>
 */
public class TextComponent<C extends TextComponent<C>> extends Component<C> {

    // Constants for text style flags
    @IntDef(flag = true, value = {
            TextFlag.NORMAL, TextFlag.BOLD, TextFlag.ITALIC,
            TextFlag.STRIKE_THRU, TextFlag.UNDERLINE, TextFlag.HIGHLIGHT
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface TextFlag {
        int NORMAL = 0;
        int BOLD = 1;
        int ITALIC = 1 << 1;
        int STRIKE_THRU = 1 << 2;
        int UNDERLINE = 1 << 3;
        int HIGHLIGHT = 1 << 4;
    }

    private String text;
    private String highlightText;
    private @TextFlag int highlightFlag;
    private @ColorInt int highlightColor;

    /**
     * {@inheritDoc}
     */
    public TextComponent(int id) {
        super(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(@NonNull IapBanner banner, View root) {
        super.init(banner, root);
        setText();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release(@NonNull IapBanner banner, View root) {
        super.release(banner, root);
    }

    /**
     * Set the text for the TextComponent.
     *
     * @param text The text to display.
     * @return The TextComponent instance to enable method chaining.
     */
    @SuppressWarnings("unchecked")
    public C text(String text) {
        this.text = text;
        return (C) this;
    }

    /**
     * Set the text to be highlighted within the TextComponent.
     *
     * @param highlightText The text to highlight.
     * @return The TextComponent instance to enable method chaining.
     */
    @SuppressWarnings("unchecked")
    public C highlightText(String highlightText) {
        this.highlightText = highlightText;
        return (C) this;
    }

    /**
     * Set the text style to normal.
     *
     * @return The TextComponent instance to enable method chaining.
     */
    @SuppressWarnings("unchecked")
    public C normal() {
        this.highlightFlag = TextFlag.NORMAL;
        this.highlightColor = Color.TRANSPARENT;
        return (C) this;
    }

    /**
     * Set the text style to bold.
     *
     * @return The TextComponent instance to enable method chaining.
     */
    @SuppressWarnings("unchecked")
    public C bold() {
        this.highlightFlag |= TextFlag.BOLD;
        return (C) this;
    }

    /**
     * Set the text style to italic.
     *
     * @return The TextComponent instance to enable method chaining.
     */
    @SuppressWarnings("unchecked")
    public C italic() {
        this.highlightFlag |= TextFlag.ITALIC;
        return (C) this;
    }

    /**
     * Set the text style to strikethrough.
     *
     * @return The TextComponent instance to enable method chaining.
     */
    @SuppressWarnings("unchecked")
    public C strikeThru() {
        this.highlightFlag |= TextFlag.STRIKE_THRU;
        return (C) this;
    }

    /**
     * Set the text style to underline.
     *
     * @return The TextComponent instance to enable method chaining.
     */
    @SuppressWarnings("unchecked")
    public C underline() {
        this.highlightFlag |= TextFlag.UNDERLINE;
        return (C) this;
    }

    /**
     * Set the text highlighting color.
     *
     * @param highlightColor The color to highlight the text.
     * @return The TextComponent instance to enable method chaining.
     */
    @SuppressWarnings("unchecked")
    public C highlight(@ColorInt int highlightColor) {
        this.highlightFlag |= TextFlag.HIGHLIGHT;
        this.highlightColor = highlightColor;
        return (C) this;
    }

    public String getText() {
        return text;
    }

    public String getHighlightText() {
        return highlightText;
    }

    public int getHighlightFlag() {
        return highlightFlag;
    }

    public int getHighlightColor() {
        return highlightColor;
    }

    /**
     * Set the text for the TextComponent, applying the specified text style.
     */
    protected void setText() {
        if (view instanceof TextView) {
            setText((TextView) view, text, highlightText, highlightFlag, highlightColor);
        }
    }

    /**
     * Set the text for a TextView with specified text style.
     *
     * @param textView       The TextView to set the text on.
     * @param text           The text to display.
     * @param highlightText  The text to highlight.
     * @param highlightFlag  The text style flags.
     * @param highlightColor The color for text highlighting.
     */
    protected void setText(TextView textView, String text, String highlightText, int highlightFlag, int highlightColor) {
        if (textView == null || text == null || text.isEmpty()) {
            return;
        }
        if (highlightFlag == TextFlag.NORMAL) {
            textView.setText(text);
            return;
        }
        int start = 0, end = 0;
        if (highlightText != null && !highlightText.isEmpty()) {
            start = text.indexOf(highlightText);
            end = start + highlightText.length();
        }
        if (start == 0) {
            end = text.length();
        }
        SpannableString sp = new SpannableString(text);

        if (hasFlag(highlightFlag, TextFlag.BOLD)) {
            StyleSpan span = new StyleSpan(Typeface.BOLD);
            sp.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (hasFlag(highlightFlag, TextFlag.ITALIC)) {
            StyleSpan span = new StyleSpan(Typeface.ITALIC);
            sp.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (hasFlag(highlightFlag, TextFlag.STRIKE_THRU)) {
            StrikethroughSpan span = new StrikethroughSpan();
            sp.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (hasFlag(highlightFlag, TextFlag.UNDERLINE)) {
            UnderlineSpan span = new UnderlineSpan();
            sp.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (hasFlag(highlightFlag, TextFlag.HIGHLIGHT)) {
            if (highlightColor != Color.TRANSPARENT) {
                ForegroundColorSpan span = new ForegroundColorSpan(highlightColor);
                sp.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        textView.setText(sp);
    }

    /**
     * Check if a given flag is set within a flag set.
     *
     * @param flag  The flag set to check.
     * @param check The flag to check for.
     * @return True if the flag is set; otherwise, false.
     */
    protected boolean hasFlag(int flag, int check) {
        return (flag & check) == check;
    }

}
