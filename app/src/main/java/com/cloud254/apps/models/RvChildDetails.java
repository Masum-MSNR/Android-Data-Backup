package com.cloud254.apps.models;

import android.view.animation.Animation;

public class RvChildDetails {
    int p, count;
    Animation animation;

    public RvChildDetails() {
    }

    public RvChildDetails(int p, int count, Animation animation) {
        this.p = p;
        this.count = count;
        this.animation = animation;
    }

    public int getP() {
        return p;
    }

    public void setP(int p) {
        this.p = p;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Animation getAnimation() {
        return animation;
    }

    public void setAnimation(Animation animation) {
        this.animation = animation;
    }
}
