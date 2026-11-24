package com.zsoftware.choresrpg.tutorial;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.zsoftware.choresrpg.R;
import com.zsoftware.choresrpg.databinding.TutorialPopupBinding;

import java.util.List;

// A short tutorial shown on top of a screen: one button at a time is highlighted inside a circular
// hole in a dark layer that covers everything else, with a small popup next to it explaining the
// button. "Next" moves on, "Skip" (or back) ends it. Taps elsewhere are ignored while it's shown.
@SuppressLint("ViewConstructor")
public class TutorialOverlay extends FrameLayout {

    // Finds the button to highlight; looked up when its step starts, because toolbar buttons only exist
    // once the screen has been laid out.
    public interface ViewFinder
    {
        View Find();
    }

    public static class Step
    {
        final ViewFinder Target;
        final int TitleId;
        final int TextId;

        public Step(ViewFinder target, int titleId, int textId)
        {
            Target = target;
            TitleId = titleId;
            TextId = textId;
        }
    }

    // How many times a step waits a frame for its button to appear before it's skipped.
    private static final int MaxFindAttempts = 20;

    private final List<Step> Steps;
    private final TutorialPopupBinding Popup;
    private final OnBackPressedCallback BackPressed;
    private final Paint DimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint RingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path DimPath = new Path();
    private int StepIndex = -1;
    // The highlight circle, in this overlay's coordinates; radius 0 = no hole.
    private float HoleX, HoleY, HoleRadius;

    // Shows the tutorial over the whole window of the activity.
    public static void Show(ComponentActivity activity, List<Step> steps)
    {
        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
        TutorialOverlay overlay = new TutorialOverlay(activity, steps);
        decor.addView(overlay, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        activity.getOnBackPressedDispatcher().addCallback(activity, overlay.BackPressed);
        overlay.ShowStep(0);
    }

    private TutorialOverlay(Context context, List<Step> steps)
    {
        super(context);
        Steps = steps;
        setWillNotDraw(false);
        setClickable(true);
        DimPaint.setColor(context.getColor(R.color.tutorial_dim));
        DimPath.setFillType(Path.FillType.EVEN_ODD);
        RingPaint.setStyle(Paint.Style.STROKE);
        RingPaint.setColor(Color.WHITE);
        RingPaint.setStrokeWidth(getResources().getDimension(R.dimen.tutorial_ring_width));

        Popup = TutorialPopupBinding.inflate(LayoutInflater.from(context), this, false);
        addView(Popup.getRoot());
        Popup.nextButton.setOnClickListener(view -> ShowStep(StepIndex + 1));
        Popup.skipButton.setOnClickListener(view -> Close());

        BackPressed = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Close();
            }
        };
    }

    private void ShowStep(int index)
    {
        if (index >= Steps.size())
        {
            Close();
            return;
        }
        StepIndex = index;
        Step step = Steps.get(index);
        Popup.getRoot().setVisibility(INVISIBLE);
        Popup.stepCounter.setText(getContext().getString(R.string.tutorial_step, index + 1, Steps.size()));
        Popup.title.setText(step.TitleId);
        Popup.text.setText(step.TextId);
        Popup.nextButton.setText(index == Steps.size() - 1 ? R.string.tutorial_done : R.string.tutorial_next);
        Popup.skipButton.setVisibility(index == Steps.size() - 1 ? GONE : VISIBLE);
        HighlightWhenReady(step, 0);
    }

    // Waits (a frame at a time) until the step's button is laid out, then highlights it.
    private void HighlightWhenReady(Step step, int attempt)
    {
        View target = step.Target.Find();
        boolean ready = target != null && target.isShown() && target.getWidth() > 0 && getWidth() > 0;
        if (!ready)
        {
            if (attempt < MaxFindAttempts)
                postOnAnimation(() -> HighlightWhenReady(step, attempt + 1));
            else
                ShowStep(StepIndex + 1);   // button not there: skip this step
            return;
        }
        Highlight(target);
    }

    private void Highlight(View target)
    {
        int[] overlayPosition = new int[2];
        int[] targetPosition = new int[2];
        getLocationOnScreen(overlayPosition);
        target.getLocationOnScreen(targetPosition);
        HoleX = targetPosition[0] - overlayPosition[0] + target.getWidth() / 2f;
        HoleY = targetPosition[1] - overlayPosition[1] + target.getHeight() / 2f;
        HoleRadius = Math.max(target.getWidth(), target.getHeight()) / 2f
                + getResources().getDimension(R.dimen.tutorial_hole_padding);
        invalidate();
        Popup.getRoot().post(this::PlacePopup);
    }

    // Next to the highlight: below it when there's room (toolbar buttons), otherwise above (the + button);
    // kept inside the screen and clear of the status / navigation bars.
    private void PlacePopup()
    {
        View card = Popup.getRoot();
        float gap = getResources().getDimension(R.dimen.tutorial_popup_gap);
        float margin = getResources().getDimension(R.dimen.tutorial_popup_margin);
        Insets bars = Insets.NONE;
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(this);
        if (insets != null)
            bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

        float y = HoleY + HoleRadius + gap;
        if (y + card.getHeight() > getHeight() - bars.bottom - margin)
            y = HoleY - HoleRadius - gap - card.getHeight();
        y = Math.max(bars.top + margin, y);

        float x = HoleX - card.getWidth() / 2f;
        x = Math.max(bars.left + margin, Math.min(getWidth() - bars.right - margin - card.getWidth(), x));

        card.setX(x);
        card.setY(y);
        card.setVisibility(VISIBLE);
    }

    private void Close()
    {
        BackPressed.remove();
        ViewGroup parent = (ViewGroup) getParent();
        if (parent != null)
            parent.removeView(this);
    }

    // The dark layer with a circular hole over the highlighted button, plus a white ring around the hole.
    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        DimPath.reset();
        DimPath.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
        if (HoleRadius > 0)
            DimPath.addCircle(HoleX, HoleY, HoleRadius, Path.Direction.CW);
        canvas.drawPath(DimPath, DimPaint);
        if (HoleRadius > 0)
            canvas.drawCircle(HoleX, HoleY, HoleRadius, RingPaint);
        super.dispatchDraw(canvas);
    }

    // Swallows taps outside the popup, so the screen underneath can't be used during the tutorial.
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }
}
