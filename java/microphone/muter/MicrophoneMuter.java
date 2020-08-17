package microphone.muter;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.IBinder;
import android.os.Handler;

public class MicrophoneMuter extends Service {

	static final int notificationID = 1;
	static final String notificationChannelID = "muter";

	Handler h = new Handler();

	boolean hasNoMicrophoneMuteChangedBroadcaster = android.os.Build.VERSION.SDK_INT < 28;
	boolean shouldPollForMicrophoneMuteChanged = hasNoMicrophoneMuteChangedBroadcaster;

	public IBinder onBind(Intent i) {
		return null;
	}

	public int onStartCommand(Intent i, int flags, int startId) {
		muteMicrophone();
		startFrontend();
		if (shouldPollForMicrophoneMuteChanged) {
			pollForMicrophoneMuteChanged();
		}
		return super.onStartCommand(i, flags, startId);
	}

	void muteMicrophone() {
		((AudioManager)getSystemService(Context.AUDIO_SERVICE))
      		.setMicrophoneMute(true);
	}

	void startFrontend() {
		createNotificationChannel();
		startForeground(
			notificationID,
			new Notification.Builder(getApplicationContext())
				.setChannelId(notificationChannelID)
				.setOngoing(true)
				.setContentTitle("All apps are currently receiving no audio from the microphone.")
				.setContentText(
					"Click this notification to re-enable the microphone for all apps."
				)
				.setContentIntent(
					PendingIntent.getActivity(this, 0, createIntent(), 0)
				)
				.setSmallIcon(R.drawable.muted_microphone_icon)
				.build()
		);
	}

	private void createNotificationChannel() {
			boolean hasNotificationChannelClass = android.os.Build.VERSION.SDK_INT >= 26;
			if (!hasNotificationChannelClass) {
				return;
			}
			NotificationChannel c = new NotificationChannel(
				notificationChannelID,
				"Microphone Muter",
				NotificationManager.IMPORTANCE_LOW
			);
			c.setDescription("Front-end for the app named Microphone Muter");
			getSystemService(NotificationManager.class)
				.createNotificationChannel(c);
	}

	public final static String keyToStop =
		"turn off the microphone muter now because the notification was clicked";

	Intent createIntent() {
		Intent i = new Intent(this, Main.class);
		i.putExtra(keyToStop, true);
		return i;
	}

	boolean isMicrophoneMuted() {
		return ((AudioManager)getSystemService(Context.AUDIO_SERVICE))
			.isMicrophoneMute();
	}

	void pollForMicrophoneMuteChanged() {
		h.postDelayed(
			new MicrophonePoller(),
			randomizeMilliseconds(500, 2000)
		);
	}

	class MicrophonePoller implements Runnable {
		public void run() {
			boolean shouldMuteMicrophone = !isMicrophoneMuted();
			if (shouldMuteMicrophone) {
				muteMicrophone();
			}
			pollForMicrophoneMuteChanged();
		}
	}

	long randomizeMilliseconds(long min, long max) {
		return (long) (java.lang.Math.random() * max) + min;
	}

	class Receiver extends BroadcastReceiver {
		// API-level 28 added AudioManager.ACTION_MICROPHONE_MUTE_CHANGED,
		// use its raw String value so we can still have backwards
		// compatibility for API-Level 25 (N / Android 7.1 / LineageOS 14).
		final String micMuteToggled = "android.media.action.MICROPHONE_MUTE_CHANGED";

		public void onReceive(Context c, Intent i) {
			boolean isMicMuteToggled = micMuteToggled.equals(i.getAction());
			boolean shouldMuteMicrophone = isMicMuteToggled && !isMicrophoneMuted();
			if (shouldMuteMicrophone) {
				muteMicrophone();
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

	// onDestroy is automatically triggered via an Activity calling:
	// `Activity.stopService(new Intent(Activity.this, MicrophoneMuter.class));`
	public void onDestroy() {
		r.unregister();
		if (shouldPollForMicrophoneMuteChanged) {
			h.removeCallbacksAndMessages(null);
		}
		if (isMicrophoneMuted()) {
			unmuteMicrophone();
		}
	}

	void unmuteMicrophone() {
		((AudioManager)getSystemService(Context.AUDIO_SERVICE))
			.setMicrophoneMute(false);
	}

}