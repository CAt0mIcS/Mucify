package com.de.mucify.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.de.mucify.R;
import com.de.mucify.UserData;
import com.de.mucify.Util;
import com.de.mucify.player.Playback;

public class FragmentMinimizedPlayer extends Fragment {

    private ImageButton mPlayPause;
    private TextView mTxtTitle;
    private TextView mTxtArtist;

    private MediaControllerActivity mMediaController;

    private final PlaybackCallback mPlaybackCallback = new PlaybackCallback();

    public FragmentMinimizedPlayer() {
        super(R.layout.fragment_minimized_player);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(Util.UncaughtExceptionLogger);
        mMediaController = (MediaControllerActivity)getActivity();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(getArguments() == null)
            throw new UnsupportedOperationException("Argument for FragmentMinimizedPlayer must be set");
        if(mMediaController == null)
            throw new UnsupportedOperationException("FragmentMinimizedPlayer must've been started with a MediaControllerActivity");
        mMediaController.addCallback(mPlaybackCallback);

        mTxtTitle = view.findViewById(R.id.txtTitle);
        mTxtArtist = view.findViewById(R.id.txtArtist);
        mTxtTitle.setText(getArguments().getString("Title"));
        mTxtArtist.setText(getArguments().getString("Subtitle"));

        mPlayPause = view.findViewById(R.id.btnPlayPause);
        mPlayPause.setOnClickListener(v -> {
            if(!mMediaController.isCreated()) {
                mMediaController.play(getArguments().getString("MediaId"));
                if(UserData.LastPlayedPlaybackPos != 0) {
                    mMediaController.seekTo(UserData.LastPlayedPlaybackPos);
                }

            }
            else if(mMediaController.isPaused())
                mMediaController.unpause();
            else
                mMediaController.pause();
        });

        // Clicking on minimized player should open the large player
        view.setOnClickListener(v -> {
            Intent i = new Intent(getActivity(), ActivityPlayer.class);
            i.putExtra("MediaId", getArguments().getString("MediaId"));

            // Player only needs title and artist if the Playback hasn't been started yet
            if(!mMediaController.isCreated()) {
                i.putExtra("Title", mTxtTitle.getText().toString());
                i.putExtra("Subtitle", mTxtArtist.getText().toString());
            }

            i.putExtra("IsPlaying", true);
            if(UserData.LastPlayedPlaybackPos != 0)
                i.putExtra("SeekPos", UserData.LastPlayedPlaybackPos);
            startActivity(i);
        });

        if(!mMediaController.isCreated() || mMediaController.isPaused())
            mPlaybackCallback.onPause();
        else
            mPlaybackCallback.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMediaController.removeCallback(mPlaybackCallback);
    }


    private class PlaybackCallback extends MediaControllerActivity.Callback {
        @Override
        public void onStart() {
            if(mPlayPause != null)
                mPlayPause.setImageResource(R.drawable.pause);
        }

        @Override
        public void onPause() {
            if(mPlayPause != null)
                mPlayPause.setImageResource(R.drawable.play);
        }

        @Override
        public void onTitleChanged(String title) {
            mTxtTitle.setText(title);
        }

        @Override
        public void onArtistChanged(String artist) {
            mTxtArtist.setText(artist);
        }
    }
}
