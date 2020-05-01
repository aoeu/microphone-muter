package microphone.muter;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;

public class Main extends Activity {

	public void onCreate(Bundle b) {
		super.onCreate(b);
		// TODO(aoeu): Read bundle-value as signal to stop of service (because notification was clicked).
		startService(new Intent(this, MicrophoneMuter.class));
		finish();
	}
}