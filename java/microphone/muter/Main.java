package microphone.muter;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.Toast;

public class Main extends Activity {

	public void onCreate(Bundle b) {
		super.onCreate(b);
		Intent i = new Intent(this, MicrophoneMuter.class);
		if (shouldStopService()) {
			stopService(i);
		} else {
			startService(i);
			showToast();
		}
		finish();
	}

	boolean shouldStopService() {
		Bundle b = getIntent().getExtras();
		return b != null && b.getBoolean(MicrophoneMuter.keyToStop, false);
	}

	void showToast() {
		String s = "The microphone is currently disabled for all Apps"
			+ " and may be re-enabled via the notification drawer.";
		Toast.makeText(this, s, Toast.LENGTH_LONG).show();
	}
}