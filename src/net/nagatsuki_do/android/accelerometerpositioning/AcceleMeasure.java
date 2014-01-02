package net.nagatsuki_do.android.accelerometerpositioning;

import java.util.ArrayList;
import java.util.List;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class AcceleMeasure implements SensorEventListener {
	
	public interface SensorEventListener {
		public void onAccuracyChanged(Sensor sensor, int accuracy);
		public void onSensorChanged(SensorEvent event, float[] accel, float[] filtered, float[] rotated, float[] magnet);
	}
	private List<SensorEventListener> listeners = new ArrayList<SensorEventListener>();
	
	private android.hardware.SensorEventListener self = this;
	
	// センサー
	private float[] accelerator = null; // 加速度
	private float[] filtered = null; // フィルタ済み加速度
	private float[] magneticField = null; // 地磁気
	
	// 計算用一時変数
	float[] converted;
	float[] gravity = new float[3]; // 推定重力加速度
	float[] rottationMatrix = new float[9]; // 回転行列
	private float vx, vy, vz; // 速度 (積分一回目)
	private float x, y, z; // 距離 (積分二回目)
	private long oldTime = System.currentTimeMillis();
	
	// 計算用定数
	private final float resetSpeed = 0.05f; // 静止判定閾値
	private final float k = 0.1f; //ローパスフィルタ係数
	
	// 総計
	private float[] result = new float[3];
	private long millisecond = 0;
	
	public void addSensorEventListener(SensorEventListener l) {
		if (listeners == null) listeners = new ArrayList<SensorEventListener>();
		listeners.add(l);
	}
	
	public void removeSensorEventListener(SensorEventListener l) {
		listeners.remove(l);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		for (SensorEventListener l : listeners) {
			l.onAccuracyChanged(sensor, accuracy);
		}
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			accelerator = event.values.clone();
			
			// ローパスフィルタとハイパスフィルタ
			gravity[0] = accelerator[0] * k + gravity[0] * (1-k);
			gravity[1] = accelerator[1] * k + gravity[1] * (1-k);
			gravity[2] = accelerator[2] * k + gravity[2] * (1-k);

			if (filtered == null) {
				filtered = new float[3];
				
				// 初回の値は捨てて後続の計算をしない
				return;
			}
			filtered[0] = accelerator[0] - gravity[0];
			filtered[1] = accelerator[1] - gravity[1];
			filtered[2] = accelerator[2] - gravity[2];
		} else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			if (magneticField == null) magneticField = new float[3];

			magneticField = event.values;
		}
		
		if (magneticField != null && filtered != null) {
			// 地球座標系への変換
			converted = coordConvert(accelerator, filtered);
			
			// 二階積分
			long newTime = event.timestamp;
			final int timeScale = 1000000;
//			long newTime = System.currentTimeMillis();
			long interval = newTime - oldTime;

			// 積分一回目(m/s^2->cm/s)
			float[] reset = new float[3];
			final float m2cm = 10;
			vx += reset[0] = converted[0] * interval / m2cm / timeScale;
			vy += reset[1] = converted[1] * interval / m2cm / timeScale;
			vz += reset[2] = converted[2] * interval / m2cm / timeScale;
			
			// 積分二回目 (cm/s->cm)
			final float ms2s = 1000;
			x += vx * interval / ms2s / timeScale;
			y += vy * interval / ms2s / timeScale;
			z += vz * interval / ms2s / timeScale;
			
			oldTime = newTime;
			millisecond += interval / timeScale;

			// 速度が十分小さければリセット
			if (Math.abs(reset[0]) < resetSpeed &&
				Math.abs(reset[1]) < resetSpeed &&
				Math.abs(reset[2]) < resetSpeed) {
				result[0] += x;
				result[1] += y;
				result[2] += z;
				x = y = z = 0;
				vx = vy = vz = 0;
			}
			
			for (SensorEventListener l : listeners) {
				l.onSensorChanged(event, accelerator, filtered, converted, magneticField);
			}
		}
	}
	
	private float[] coordConvert(float[] raw, float[] linear) {
		float[] rotated;
		float[] I = new float[9];
		SensorManager.getRotationMatrix(rottationMatrix, I, raw, magneticField);
		SensorManager.remapCoordinateSystem(rottationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Y, rottationMatrix);
		rotated = productMV(rottationMatrix, linear);
		return rotated;
	}
	
	private float[] productMV(float[] lhs, float[] rhs) {
		float[] producted = new float[3];
		producted[0] = lhs[0] * rhs[0] + lhs[1] * rhs[1] + lhs[2] * rhs[2];
		producted[1] = lhs[3] * rhs[0] + lhs[4] * rhs[1] + lhs[5] * rhs[2];
		producted[2] = lhs[6] * rhs[0] + lhs[7] * rhs[1] + lhs[8] * rhs[2];
		return producted;
	}

	void reset() {
		x = y = z = 0;
		vx = vy = vz = 0;
		result[0] = result[1] = result[2] = 0;
		millisecond = 0;
	}
	
	void start(SensorManager sensorManager, int delay) {
		List<Sensor> accList = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
		List<Sensor> magList = sensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
		if (accList.size() > 0 && magList.size() > 0) {
			sensorManager.registerListener(self, accList.get(0), delay);
			sensorManager.registerListener(self, magList.get(0), delay);
		}
	}
	
	void stop(SensorManager sensorManager) {
		List<Sensor> accList = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
		List<Sensor> magList = sensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
		if (accList.size() > 0 && magList.size() > 0) {
			sensorManager.unregisterListener(self, accList.get(0));
			sensorManager.unregisterListener(self, magList.get(0));
			sensorManager = null;
		}
		reset();
	}
	
	float[] filtered() {
		return filtered;
	}
	
	float[] rotated() {
		return converted;
	}
	
	float[] result() {
		return result;
	}
	
	public String toString() {
		if (rottationMatrix == null || filtered == null || converted == null || result == null) return "";

		float[] rotationVector = new float[3];
		SensorManager.getOrientation(rottationMatrix, rotationVector);
		String s = "RotationMatrix:"
				 + "\n    Yaw  : " + Math.toDegrees(rotationVector[0])
				 + "\n    Pitch: " + Math.toDegrees(rotationVector[1])
				 + "\n    Roll : " + Math.toDegrees(rotationVector[2])
				 + "\nAccelerometer:"
				 + "\n    Axis X: " + filtered[0]
				 + "\n    Axis Y: " + filtered[1]
				 + "\n    Axis Z: " + filtered[2]
		         + "\nConverted:"
				 + "\n    Axis X: " + converted[0]
				 + "\n    Axis Y: " + converted[1]
				 + "\n    Axis Z: " + converted[2]
				 + "\nCoord:"
				 + "\n    X: " + x
				 + "\n    Y: " + y
				 + "\n    Z: " + z
				 + "\nResult:"
				 + "\n    X: " + result[0]
				 + "\n    Y: " + result[1]
				 + "\n    Z: " + result[2]
				 + "\nSecond: " + millisecond / 1000;
		Log.v("Accelerometer", filtered[0] + "," + filtered[1] + "," + filtered[2] + "," + converted[0] + "," + converted[1] + "," + converted[2] + "," + x + "," + y + "," + z);
		
		return s;
	}
}
