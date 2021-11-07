package com.de.mucify.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.de.mucify.R;
import com.de.mucify.playable.AudioController;


public class ForegroundNotificationClickReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(!intent.hasExtra("PlayPause"))
            return;

        switch(intent.getAction()) {
            case "com.de.mucify.PLAY_PAUSE":
                if(intent.getIntExtra("PlayPause", 0) == R.drawable.ic_black_pause)
                    AudioController.get().pauseSong();
                else if(intent.getIntExtra("PlayPause", 0) ==  R.drawable.ic_black_play)
                    AudioController.get().unpauseSong();
                break;
        }
    }
}
