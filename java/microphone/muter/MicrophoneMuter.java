package microphone.muter;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;

public class MicrophoneMuter extends Service {

	static final int notificationID = 1;
	static final String notificationChannelID = "muter";

	public IBinder onBind(Intent i) {
		return null;
	}

	public int onStartCommand(Intent i, int flags, int startId) {
		muteMicrophone();
		startFrontend();
		return super.onStartCommand(i, flags, startId);
	}

	// TODO(aoeu): Add broadcast receiver to re-mute microphone if unmuted.
	// https://developer.android.com/reference/android/media/AudioManager?#ACTION_MICROPHONE_MUTE_CHANGED

	void muteMicrophone() {
		((AudioManager)getSystemService(Context.AUDIO_SERVICE))
      		.setMicrophoneMute(true);
	}

	void startFrontend() {
		startForeground(
			notificationID,

			new Notification.Builder(getApplicationContext())
				.setOngoing(true)
				.setContentTitle("Microphone is muted")
				.setContentText("All apps will receive no audio from the microphone.")
				// TODO(aoeu): Add bundle-value to signal stopping of service when notification is clicked.
				.setContentIntent(
					PendingIntent.getActivity(this, 0, new Intent(this, Main.class), 0)
				)
				.setSmallIcon(android.R.drawable.ic_lock_silent_mode)
				.build()
		);
	}
}