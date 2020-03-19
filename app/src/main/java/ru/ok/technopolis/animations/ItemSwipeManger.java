package ru.ok.technopolis.animations;

import android.content.Context;

import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FlingAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.HashMap;
import java.util.Map;

public class ItemSwipeManger implements RecyclerView.OnItemTouchListener, RecyclerView.OnChildAttachStateChangeListener {

    private final Map<RecyclerView.ViewHolder, DynamicAnimation> animations = new HashMap<>();
    private final SwipeListener listener;
    private final int touchSlop;
    private float initialTouchX;
    private RecyclerView recyclerView;
    private VelocityTracker velocityTracker;
    private View swipedChild;

    public ItemSwipeManger(@NonNull Context context, @NonNull SwipeListener listener) {
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        this.listener = listener;
    }

    public void attachToRecyclerView(@NonNull RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        recyclerView.addOnItemTouchListener(this);
        recyclerView.addOnChildAttachStateChangeListener(this);
    }

    public void detachFromRecyclerView() {
        recyclerView.removeOnItemTouchListener(this);
        recyclerView.removeOnChildAttachStateChangeListener(this);
        for (DynamicAnimation animation : animations.values()) {
            animation.cancel();
        }
        animations.clear();
        recyclerView = null;
    }

    @Override
    public void onChildViewAttachedToWindow(@NonNull View view) {
    }

    @Override
    public void onChildViewDetachedFromWindow(@NonNull View view) {
        view.setTranslationX(0);
        RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(view);
        if (holder == null) {
            return;
        }
        animations.remove(holder);
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                initialTouchX = event.getX();
                if (velocityTracker != null) {
                    velocityTracker.recycle();
                }
                velocityTracker = VelocityTracker.obtain();
                velocityTracker.addMovement(event);
                return false;
            case MotionEvent.ACTION_MOVE:
                velocityTracker.addMovement(event);
                boolean dragged = event.getX() - initialTouchX > touchSlop;
                if (dragged) {
                    swipedChild = rv.findChildViewUnder(event.getX(), event.getY());
                }
                return dragged;
        }
        return false;
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent event) {
        if (swipedChild == null) {
            return;
        }
        velocityTracker.addMovement(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                swipedChild.setTranslationX(event.getX() - initialTouchX);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                RecyclerView.ViewHolder swipeViewHolder = rv.findContainingViewHolder(swipedChild);
                if (swipeViewHolder == null) {
                    return;
                }
                velocityTracker.computeCurrentVelocity(1000);
                float velocity = velocityTracker.getXVelocity();
                if (velocity > 0) {
                    animateWithFling(swipeViewHolder, velocity);
                } else {
                    animateWithSpring(swipeViewHolder, velocity);
                }
                velocityTracker.clear();
                break;
        }
    }

    private void animateWithFling(@NonNull final RecyclerView.ViewHolder viewHolder, float velocity) {
        FlingAnimation animation = new FlingAnimation(swipedChild, DynamicAnimation.TRANSLATION_X);
        animation.setFriction(1f);
        animation.setStartVelocity(velocity);
        animation.setMaxValue(swipedChild.getWidth());
        viewHolder.setIsRecyclable(false);
        animation.addEndListener(new DynamicAnimation.OnAnimationEndListener() {
            @Override
            public void onAnimationEnd(DynamicAnimation animation, boolean canceled, float value, float velocity) {
                if (value >= recyclerView.getWidth()) {
                    viewHolder.setIsRecyclable(true);
                    listener.onSwiped(viewHolder);
                } else {
                    animateWithSpring(viewHolder, velocity);
                }
            }
        });
        animations.put(viewHolder, animation);
        animation.start();
    }

    private void animateWithSpring(@NonNull final RecyclerView.ViewHolder viewHolder, float velocity) {
        SpringAnimation animation = new SpringAnimation(viewHolder.itemView, DynamicAnimation.TRANSLATION_X);
        animation.setStartVelocity(velocity);
        SpringForce springForce = new SpringForce(0);
        springForce.setDampingRatio(SpringForce.DAMPING_RATIO_LOW_BOUNCY);
        springForce.setStiffness(SpringForce.STIFFNESS_LOW);
        animation.setSpring(springForce);
        viewHolder.setIsRecyclable(false);
        animation.addEndListener(new DynamicAnimation.OnAnimationEndListener() {
            @Override
            public void onAnimationEnd(DynamicAnimation animation, boolean canceled, float value, float velocity) {
                animations.remove(viewHolder);
                viewHolder.setIsRecyclable(true);
            }
        });
        animations.put(viewHolder, animation);
        animation.start();
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }

    interface SwipeListener {

        void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder);

    }

}
