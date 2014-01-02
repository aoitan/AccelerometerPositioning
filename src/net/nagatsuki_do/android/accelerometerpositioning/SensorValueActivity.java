package net.nagatsuki_do.android.accelerometerpositioning;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

public class SensorValueActivity extends Activity implements AcceleMeasure.SensorEventListener {

	private DrawTask task = null;
	private AcceleMeasure measure = null;
	private XYChartView chart = null;
	private int delay = SensorManager.SENSOR_DELAY_NORMAL;

	@Override
	protected void onStart() {
		super.onStart();
		
		measure = new AcceleMeasure();
		measure.addSensorEventListener(this);

		setContentView(R.layout.value_view);
		Button reset = (Button)findViewById(R.id.reset_button);
		reset.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				measure.reset();
				chart.reset();
			}
		});
		Button start = (Button)findViewById(R.id.start_button);
		start.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				measure.start((SensorManager)getSystemService(SENSOR_SERVICE), delay);
				task = new DrawTask();
				task.execute();
			}
		});
		Button stop = (Button)findViewById(R.id.stop_button);
		stop.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				measure.stop((SensorManager)getSystemService(SENSOR_SERVICE));
				task.cancel(true);
			}
		});
		Spinner spiner = (Spinner)findViewById(R.id.spiner);
		spiner.setSelection(3);
		spiner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				delay = pos;
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				
			}
		});
		chart = (XYChartView)findViewById(R.id.chart);
		chart.createChart();
		chart.setChartPoints(100);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	private class DrawTask extends AsyncTask<Integer, Integer, Integer> {
		private final long interval = 100;
		
		@Override
		protected Integer doInBackground(Integer... params) {
			while (true) {
				SystemClock.sleep(interval);
				publishProgress();
				if (isCancelled()) break;
			}
			
			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);

			// 情報表示(テキスト)
			String s = measure.toString();

			TextView tv = (TextView)findViewById(R.id.value_text);
			tv.setText(s);
			
			// 情報表示(グラフ)
			chart.chartChanged(null);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
	}

	@Override
	public void onSensorChanged(SensorEvent event, float[] accel,
			float[] filtered, float[] rotated, float[] magnet) {
		if (event    == null ||
			accel    == null ||
			filtered == null ||
			rotated  == null ||
			magnet   == null) return;
		// 情報表示(グラフ)
		final long timeScale = 1000000; // ns2ms
		long time = event.timestamp / timeScale;
		chart.addPoint(0, time, rotated[0]); // rotated X Axis
		chart.addPoint(1, time, rotated[1]); // rotated Y Axis
		chart.addPoint(2, time, rotated[2]); // rotated Z Axis
		chart.seriesChanged();
	}
	
}
