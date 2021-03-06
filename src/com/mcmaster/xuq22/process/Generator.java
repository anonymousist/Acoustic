package com.mcmaster.xuq22.process;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.util.Log;

public class Generator extends AsyncTask<Integer, Void, Void> {
	public static final byte marker = 10;
	public static final byte escaper = 11;
	private AudioTrack track;
	private byte[] source;
	
	public Generator(byte[] source){
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		//Write the marker
		baos.write(marker);
		
		//Write the actual contents
		for (byte i : source){
			baos.write(i);
		}
		
		//Calculate the crc32 checksum
		Checksum checksum = new CRC32();
		// update the current checksum with the specified array of bytes
		checksum.update(source, 0, source.length);
		// get the current checksum value
		long checksumValue = checksum.getValue();
		byte[] checksumBytes = Arrays.copyOfRange(ByteBuffer.allocate(8).putLong(checksumValue).array(), 0, 8);
		
		//Write the actual contents
		for (byte i : checksumBytes){
			baos.write(i);
		}
		
		this.source = baos.toByteArray();
	}

	@Override
	protected Void doInBackground(Integer... integers) {
		int[] freqs = new int[] {17700, 17900, 18100, 18300 }; 
		int sampleCount = 441;
		short[][] samples = new short[4][sampleCount];
		double baseAng = 0;
		double baseInc = (2 * Math.PI) * 50 / Common.SAMPLE_RATE;
		double ang = 0;
		double[] incs = new double[freqs.length];
		for (int i = 0; i < freqs.length; i++) {
			incs[i] = (2 * Math.PI) * freqs[i] / Common.SAMPLE_RATE;
			ang = 0;
			baseAng = 0;
			for (int j = 0; j < sampleCount; j++){
				samples[i][j] = (short)(Math.sin(ang)*Short.MAX_VALUE*Math.sin(baseAng)/16);
				ang+= incs[i];
				baseAng += baseInc;
			}
		}
		int minBufferSize = AudioTrack.getMinBufferSize(Common.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
		this.track = new AudioTrack(AudioManager.STREAM_MUSIC,
				Common.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
				AudioFormat.ENCODING_PCM_16BIT, minBufferSize, AudioTrack.MODE_STREAM);
		short[] buffer = new short[minBufferSize];
		this.track.play();
		int bufferIndex = 0;
		int sampleIndex = 0;
		while (!this.isCancelled()) {
			for (int i=0; i < source.length; i++){
				for (int j = 0; j<4;){
					buffer[bufferIndex] = samples[source[i]>>>(3-j)*2&3][sampleIndex];
					bufferIndex++;
					sampleIndex++;
					if (sampleIndex >= sampleCount){
						sampleIndex = 0;
						j++;
					}
					if (bufferIndex >= minBufferSize){
						bufferIndex = 0;
						this.track.write(buffer, 0, minBufferSize);
					}
				}
			}
			//Log.i("send", "sent");
			  try {
				TimeUnit.MILLISECONDS.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		this.track.flush();
		this.track.stop();
		return null;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}

	@Override
	protected void onPostExecute(Void aVoid) {
		super.onPostExecute(aVoid);
	}

	@Override
	protected void onProgressUpdate(Void... values) {
		super.onProgressUpdate(values);
	}

	@Override
	protected void onCancelled(Void aVoid) {
		if (this.track != null){
			this.track.flush();
			this.track.stop();
		}
		
	}

	@Override
	protected void onCancelled() {
		super.onCancelled();
	}
}
