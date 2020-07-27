package microphone.muter;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;

public class Main extends Activity {

	public void onCreate(Bundle b) {
		super.onCreate(b);
		Intent i = new Intent(this, MicrophoneMuter.class);
		if (shouldStopService()) {
			stopService(i);
		} else {
			startService(i);
		}
		finish();
	}

	boolean shouldStopService() {
		Bundle b = getIntent().getExtras();
		return b != null && b.getBoolean(MicrophoneMuter.keyToStop, false);
	}
}