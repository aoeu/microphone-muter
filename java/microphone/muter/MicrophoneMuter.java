package microphone.muter;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

	boolean isMicrophoneMuted() {
		return ((AudioManager)getSystemService(Context.AUDIO_SERVICE))
			.isMicrophoneMute();
	}

	// TODO(aoeu): Is there any way to receive broadcasts if mic mute is toggled in API 25?
	class Receiver extends BroadcastReceiver {
		// API-level 28 added AudioManager.ACTION_MICROPHONE_MUTE_CHANGED,
		// use its raw String value so we can still have backwards
		// compatibility for API-Level 25 (N / Android 7.1 / LineageOS 14).
		final String micMuteToggled = "android.media.action.MICROPHONE_MUTE_CHANGED";

		public void onReceive(Context c, Intent i) {
			if (micMuteToggled.equals(i.getAction())) {
				if (!isMicrophoneMuted()) {
					muteMicrophone();
				}
			}
		}

		public void register() {
			IntentFilter i = new IntentFilter();
			i.addAction(micMuteToggled);
			registerReceiver(this, i);
		}

		public void unregister() {
			unregisterReceiver(this);
		}
	};

	final Receiver r = new Receiver();

	public void onCreate() {
		r.register();
	}

	public void onDestroy() {
		r.unregister();
	}
}