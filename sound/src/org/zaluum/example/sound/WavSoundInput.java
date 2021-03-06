package org.zaluum.example.sound;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.zaluum.annotation.Apply;
import org.zaluum.annotation.Box;

@Box
public class WavSoundInput {
	static final int channels = 2;
	static final int bits = 16;
	static final float sampleRate = 44100.0f;
	AudioFormat format = new AudioFormat(sampleRate, bits, channels, true, false);
	byte[] buffer;
	AudioInputStream audioInputStream;
	boolean end = false;
	private final String file;
	public WavSoundInput(String file) throws LineUnavailableException,
			UnsupportedAudioFileException, IOException {
		
		this.file = file;
		buffer = new byte[format.getFrameSize()];
		open();
	}
	public void open() throws UnsupportedAudioFileException, IOException {
		AudioInputStream ais = AudioSystem.getAudioInputStream(new File(file));
		audioInputStream = AudioSystem.getAudioInputStream(format, ais);
	}
	double[] chunk = new double[512];
	@Apply
	public double[] chunk() {
		for (int i=0;i<chunk.length;i++){
			chunk[i]=read();
		}
		return chunk;
	}
	@Apply
	public double read() {
		double out;
		try {
			int read = audioInputStream.read(buffer);
			if (read == -1) {
				audioInputStream.close();
				open();
				out = 0;
			} else {
				int sample = (short) (((int) buffer[1] & 0xff) << 8)
						+ ((int) buffer[0] & 0xff);
				out = (sample) / 32767.0;
			}
		} catch (IOException e) {
			e.printStackTrace();
			out = 0;
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
			out = 0;
		}
		return out;
	}

	public void test() throws Exception {
		read();
	}

	public static void main(String[] args) throws Exception {
		for (Info i : AudioSystem.getMixerInfo()) {
			System.out.println(i);
			Mixer m = AudioSystem.getMixer(i);
			System.out.println("sources");
			for (javax.sound.sampled.Line.Info linfo : m.getSourceLineInfo()) {
				System.out.println("\t" + linfo);
			}
			System.out.println("targets");
			for (javax.sound.sampled.Line.Info linfo : m.getTargetLineInfo()) {
				System.out.println("\t" + linfo);
			}

		}
		new WavSoundInput("easy-11025-mono.wav").test();
	}
}
