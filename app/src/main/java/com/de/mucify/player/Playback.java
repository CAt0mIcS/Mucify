package com.de.mucify.player;


import android.content.Context;

public abstract class Playback {
    protected Callback mCallback;

    interface Callback {

    }

    void setCallback(Callback callback) { mCallback = callback; }

    public abstract void start();
    public abstract void unpause();
    public abstract void pause();
    public abstract boolean isPlaying();
    public abstract boolean isPaused();
    public abstract void seekTo(int millis);
    public abstract int getDuration();
    public abstract int getCurrentPosition();
    public abstract void stop();
    public abstract void reset();
    public abstract void create(Context context);

    public abstract String getTitle();
    public abstract String getSubtitle();
}
