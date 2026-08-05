package dev.omialien.voicechatrecording.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class AudioEffect {

    public enum Effect {
        PITCH, REVERB, ROBOT, REVERSE, STUTTER, HIGH_PASS, ECHO, GLITCH, MULTI_PITCH
    }

    private final Set<Effect> excludedEffects = EnumSet.noneOf(Effect.class);

    private float pitchFactor;
    private float reverbDecay;
    private int reverbDelayMs;
    private int reverbRepeats;
    private boolean pitchEnabled, reverbEnabled;
    private static final int SAMPLE_RATE = 48000;
    private boolean robotEnabled;
    private float robotLfoFreq;

    private boolean reverseEnabled;

    private boolean stutterEnabled;
    private float stutterChunkSeconds;
    private int stutterRepeats;

    private boolean highPassEnabled;
    private float highPassCutoffHz;

    private boolean echoEnabled;
    private float echoDecay;
    private int echoDelayMs;
    private int echoRepeats;

    private boolean glitchEnabled;
    private float glitchStartChunkSeconds;
    private float glitchMinChunkSeconds;
    private int glitchStages;

    private boolean multiPitchEnabled;
    private float multiPitchHighFactor;
    private float multiPitchLowFactor;


    public AudioEffect(){
        this.pitchFactor = 1;
        this.reverbDecay = 0;
        this.reverbDelayMs = 0;
        this.reverbRepeats = 0;
        this.pitchEnabled = false;
        this.reverbEnabled = false;
    }

    /**
     * Stops this effect from being picked by random(). Works whether you call it
     * before or after random()
     */
    public AudioEffect exclude(Effect effect) {
        excludedEffects.add(effect);
        disableEffect(effect);
        return this;
    }

    /**
     * Undoes a previous exclude().
     */
    public AudioEffect include(Effect effect) {
        excludedEffects.remove(effect);
        return this;
    }

    public boolean isExcluded(Effect effect) {
        return excludedEffects.contains(effect);
    }

    private void disableEffect(Effect effect) {
        switch (effect) {
            case PITCH -> pitchEnabled = false;
            case REVERB -> reverbEnabled = false;
            case ROBOT -> robotEnabled = false;
            case REVERSE -> reverseEnabled = false;
            case STUTTER -> stutterEnabled = false;
            case HIGH_PASS -> highPassEnabled = false;
            case ECHO -> echoEnabled = false;
            case GLITCH -> glitchEnabled = false;
            case MULTI_PITCH -> multiPitchEnabled = false;
        }
    }

    public static AudioEffect pitch(float pitchFactor){
        return new AudioEffect().changePitch(pitchFactor);
    }

    public AudioEffect changePitch(float pitchFactor) {
        this.pitchFactor = pitchFactor;
        this.pitchEnabled = true;
        return this;
    }

    public static AudioEffect reverb(float decay, int delayMs, int repeats){
        return new AudioEffect().makeReverb(decay, delayMs, repeats);
    }

    public AudioEffect makeReverb(float decay, int delayMs, int repeats) {
        this.reverbDecay = decay;
        this.reverbDelayMs = delayMs;
        this.reverbRepeats = repeats;
        this.reverbEnabled = true;
        return this;
    }

    public static AudioEffect robot(float lfoFreqHz){
        return new AudioEffect().makeRobot(lfoFreqHz);
    }

    public AudioEffect makeRobot(float lfoFreqHz) {
        this.robotEnabled = true;
        this.robotLfoFreq = lfoFreqHz;
        return this;
    }

    public static AudioEffect reverse(){
        return new AudioEffect().makeReverse();
    }

    public AudioEffect makeReverse() {
        this.reverseEnabled = true;
        return this;
    }

    public static AudioEffect stutter(){
        return stutter(0.3f, 3);
    }

    public static AudioEffect stutter(float chunkSeconds, int repeats){
        return new AudioEffect().makeStutter(chunkSeconds, repeats);
    }

    public AudioEffect makeStutter(float chunkSeconds, int repeats) {
        this.stutterChunkSeconds = chunkSeconds;
        this.stutterRepeats = repeats;
        this.stutterEnabled = true;
        return this;
    }

    public static AudioEffect highPass(){
        return highPass(1000f);
    }

    public static AudioEffect highPass(float cutoffHz){
        return new AudioEffect().makeHighPass(cutoffHz);
    }

    public AudioEffect makeHighPass(float cutoffHz) {
        this.highPassCutoffHz = cutoffHz;
        this.highPassEnabled = true;
        return this;
    }

    public static AudioEffect echo(){
        return echo(0.4f, 300, 4);
    }

    public static AudioEffect echo(float decay, int delayMs, int repeats){
        return new AudioEffect().makeEcho(decay, delayMs, repeats);
    }

    public AudioEffect makeEcho(float decay, int delayMs, int repeats) {
        this.echoDecay = decay;
        this.echoDelayMs = delayMs;
        this.echoRepeats = repeats;
        this.echoEnabled = true;
        return this;
    }

    public static AudioEffect glitch(){
        return glitch(0.6f, 0.1f, 6);
    }

    public static AudioEffect glitch(float startChunkSeconds, float minChunkSeconds, int stages){
        return new AudioEffect().makeGlitch(startChunkSeconds, minChunkSeconds, stages);
    }

    public AudioEffect makeGlitch(float startChunkSeconds, float minChunkSeconds, int stages) {
        this.glitchStartChunkSeconds = startChunkSeconds;
        this.glitchMinChunkSeconds = minChunkSeconds;
        this.glitchStages = stages;
        this.glitchEnabled = true;
        return this;
    }

    public static AudioEffect multiPitch(){
        return multiPitch(1.6f, 0.6f);
    }

    public static AudioEffect multiPitch(float highFactor, float lowFactor){
        return new AudioEffect().makeMultiPitch(highFactor, lowFactor);
    }

    public AudioEffect makeMultiPitch(float highFactor, float lowFactor) {
        this.multiPitchHighFactor = highFactor;
        this.multiPitchLowFactor = lowFactor;
        this.multiPitchEnabled = true;
        return this;
    }

    public static AudioEffect random(){
        return new AudioEffect().addRandomEffects();
    }

    public AudioEffect addRandomEffects() {
        Random random = new Random();

        List<Effect> available = new ArrayList<>(Arrays.asList(Effect.values()));
        available.removeAll(excludedEffects);

        if (available.isEmpty()) {
            return this;
        }

        boolean added = false;
        for (Effect effect : available) {
            if (random.nextBoolean()) {
                applyRandomEffect(effect);
                added = true;
            }
        }

        if (!added) {
            Effect forced = available.get(random.nextInt(available.size()));
            applyRandomEffect(forced);
        }

        return this;
    }

    private void applyRandomEffect(Effect effect) {
        switch (effect) {
            case PITCH -> changePitch(0.7f);
            case REVERB -> makeReverb(0.5f, 160, 2);
            case ROBOT -> makeRobot(30f);
            case REVERSE -> makeReverse();
            case STUTTER -> makeStutter(0.3f, 3);
            case HIGH_PASS -> makeHighPass(1000f);
            case ECHO -> makeEcho(0.4f, 300, 4);
            case GLITCH -> makeGlitch(0.6f, 0.1f, 6);
            case MULTI_PITCH -> makeMultiPitch(1.6f, 0.6f);
        }
    }

    public short[] applyEffects(short[] pcm) {
        if (pitchEnabled) pcm = changePitch(pcm, pitchFactor);
        if (multiPitchEnabled) pcm = multiPitch(pcm, multiPitchHighFactor, multiPitchLowFactor);
        if (robotEnabled) pcm = robotize(pcm, robotLfoFreq);
        if (highPassEnabled) pcm = highPass(pcm, highPassCutoffHz);
        if (stutterEnabled) pcm = stutter(pcm, stutterChunkSeconds, stutterRepeats);
        if (glitchEnabled) pcm = glitch(pcm, glitchStartChunkSeconds, glitchMinChunkSeconds, glitchStages);
        if (reverseEnabled) pcm = reverseAudio(pcm);
        if (echoEnabled) pcm = echo(pcm, echoDecay, echoDelayMs, echoRepeats);
        if (reverbEnabled) pcm = addReverb(pcm, reverbDecay, reverbDelayMs, reverbRepeats);
        return pcm;
    }

    /**
     * Changes the pitch of the audio, also makes it faster/slower.
     */
    private static short[] changePitch(short[] pcm, float pitchFactor) {
        if (pitchFactor <= 0) throw new IllegalArgumentException("Pitch factor must be > 0");

        int newLength = (int)(pcm.length / pitchFactor);
        short[] result = new short[newLength];

        for (int i = 0; i < newLength; i++) {
            float srcIndex = i * pitchFactor;
            int index = (int) srcIndex;
            float frac = srcIndex - index;

            if (index + 1 < pcm.length) {
                result[i] = (short)((1 - frac) * pcm[index] + frac * pcm[index + 1]);
            } else {
                result[i] = pcm[index];
            }
        }

        return result;
    }
    /**
     * Adds reverb to the audio.
     */
    private static short[] addReverb(short[] input, float decay, int delayMs, int repeats) {
        if (decay <= 0 || decay >= 1) throw new IllegalArgumentException("Decay must be between 0 and 1");
        if (delayMs <= 0 || repeats <= 0) throw new IllegalArgumentException("Delay and repeats must be > 0");

        int delaySamples = (SAMPLE_RATE * delayMs) / 1000;

        int totalLength = input.length + delaySamples * repeats;
        short[] output = new short[totalLength];

        System.arraycopy(input, 0, output, 0, input.length);

        for (int r = 1; r <= repeats; r++) {
            int offset = delaySamples * r;
            float currentDecay = (float) Math.pow(decay, r);

            for (int i = 0; i < input.length; i++) {
                int delayedIndex = i + offset;
                if (delayedIndex >= output.length) break;
                int mixed = output[delayedIndex] + (int) (input[i] * currentDecay);
                output[delayedIndex] = (short) Math.max(Math.min(mixed, Short.MAX_VALUE), Short.MIN_VALUE);
            }
        }

        return output;
    }
    /**
     * Makes the player's voice sound like a robot.
     */
    private static short[] robotize(short[] pcm, float lfoFreqHz) {
        if (lfoFreqHz <= 0) throw new IllegalArgumentException("LFO frequency must be > 0");

        short[] output = new short[pcm.length];
        double lfoPhase = 0;
        double lfoIncrement = 2.0 * Math.PI * lfoFreqHz / SAMPLE_RATE;

        for (int i = 0; i < pcm.length; i++) {
            double modulator = Math.cos(lfoPhase);
            lfoPhase += lfoIncrement;
            if (lfoPhase >= 2.0 * Math.PI) lfoPhase -= 2.0 * Math.PI;

            int sample = (int) (pcm[i] * modulator);
            if (sample > Short.MAX_VALUE) sample = Short.MAX_VALUE;
            else if (sample < Short.MIN_VALUE) sample = Short.MIN_VALUE;

            output[i] = (short) sample;
        }

        return output;
    }

    /**
     * Plays the audio backwards.
     */
    private static short[] reverseAudio(short[] pcm) {
        short[] output = new short[pcm.length];
        for (int i = 0; i < pcm.length; i++) {
            output[i] = pcm[pcm.length - 1 - i];
        }
        return output;
    }

    /**
     * Repeats one chunk of the audio a few times in a row. The chunk is picked
     * randomly but never too close to the start or end.
     */
    private static short[] stutter(short[] pcm, float chunkSeconds, int repeats) {
        if (chunkSeconds <= 0) throw new IllegalArgumentException("Chunk seconds must be > 0");
        if (repeats <= 0) throw new IllegalArgumentException("Repeats must be > 0");

        int chunkSamples = (int) (SAMPLE_RATE * chunkSeconds);
        if (chunkSamples <= 0) throw new IllegalArgumentException("Chunk seconds too small");

        int margin = Math.max(chunkSamples * 2, pcm.length / 10);

        int minStart = margin;
        int maxStart = pcm.length - margin - chunkSamples;

        if (maxStart <= minStart) {
            return pcm;
        }

        Random random = new Random();
        int stutterStart = minStart + random.nextInt(maxStart - minStart);
        int stutterEnd = stutterStart + chunkSamples;

        short[] chunk = Arrays.copyOfRange(pcm, stutterStart, stutterEnd);

        int extraLength = chunkSamples * repeats;
        short[] output = new short[pcm.length + extraLength];

        System.arraycopy(pcm, 0, output, 0, stutterEnd);

        int writePos = stutterEnd;
        for (int r = 0; r < repeats; r++) {
            System.arraycopy(chunk, 0, output, writePos, chunk.length);
            writePos += chunk.length;
        }

        System.arraycopy(pcm, stutterEnd, output, writePos, pcm.length - stutterEnd);

        return output;
    }

    /**
     * Cuts the low end out of the audio.
     */
    private static short[] highPass(short[] pcm, float cutoffHz) {
        if (cutoffHz <= 0) throw new IllegalArgumentException("Cutoff frequency must be > 0");
        if (pcm.length == 0) return pcm;

        float rc = 1.0f / (2 * (float) Math.PI * cutoffHz);
        float dt = 1.0f / SAMPLE_RATE;
        float alpha = rc / (rc + dt);

        short[] output = new short[pcm.length];
        float prevInput = pcm[0];
        float prevOutput = pcm[0];
        output[0] = pcm[0];

        for (int i = 1; i < pcm.length; i++) {
            float currentOutput = alpha * (prevOutput + pcm[i] - prevInput);
            output[i] = (short) Math.max(Math.min(currentOutput, Short.MAX_VALUE), Short.MIN_VALUE);
            prevInput = pcm[i];
            prevOutput = currentOutput;
        }

        return output;
    }

    /**
     * Adds a few fading repeats of the audio after itself
     */
    private static short[] echo(short[] input, float decay, int delayMs, int repeats) {
        if (decay <= 0 || decay >= 1) throw new IllegalArgumentException("Decay must be between 0 and 1");
        if (delayMs <= 0 || repeats <= 0) throw new IllegalArgumentException("Delay and repeats must be > 0");

        int delaySamples = (SAMPLE_RATE * delayMs) / 1000;
        int totalLength = input.length + delaySamples * repeats;
        short[] output = new short[totalLength];

        System.arraycopy(input, 0, output, 0, input.length);

        for (int r = 1; r <= repeats; r++) {
            int offset = delaySamples * r;
            float currentDecay = decay / r;

            for (int i = 0; i < input.length; i++) {
                int delayedIndex = i + offset;
                if (delayedIndex >= output.length) break;
                int mixed = output[delayedIndex] + (int) (input[i] * currentDecay);
                output[delayedIndex] = (short) Math.max(Math.min(mixed, Short.MAX_VALUE), Short.MIN_VALUE);
            }
        }

        return output;
    }

    /**
     * Plays normally, then near the end starts repeating a shrinking piece of the
     * tail faster and faster until it's stuck on a tiny fragment and the audio ends.
     */
    private static short[] glitch(short[] pcm, float startChunkSeconds, float minChunkSeconds, int stages) {
        if (startChunkSeconds <= 0 || minChunkSeconds <= 0)
            throw new IllegalArgumentException("Chunk seconds must be > 0");
        if (minChunkSeconds >= startChunkSeconds)
            throw new IllegalArgumentException("minChunkSeconds must be smaller than startChunkSeconds");
        if (stages < 1) throw new IllegalArgumentException("Stages must be >= 1");
        if (pcm.length == 0) return pcm;

        int startChunkSamples = (int) (SAMPLE_RATE * startChunkSeconds);
        int minChunkSamples = Math.max(1, (int) (SAMPLE_RATE * minChunkSeconds));

        if (startChunkSamples >= pcm.length) {
            startChunkSamples = Math.max(pcm.length / 2, minChunkSamples + 1);
        }

        int glitchPoint = pcm.length - startChunkSamples;

        int[] chunkSamplesPerStage = new int[stages];
        int[] repeatsPerStage = new int[stages];
        int tailLength = 0;

        for (int stage = 0; stage < stages; stage++) {
            float t = (stages == 1) ? 1f : stage / (float) (stages - 1);
            int chunkSamples = Math.max(minChunkSamples,
                    (int) (startChunkSamples - t * (startChunkSamples - minChunkSamples)));
            int repeats = 2 + stage;

            chunkSamplesPerStage[stage] = chunkSamples;
            repeatsPerStage[stage] = repeats;
            tailLength += chunkSamples * repeats;
        }

        short[] output = new short[glitchPoint + tailLength];
        System.arraycopy(pcm, 0, output, 0, glitchPoint);

        int writePos = glitchPoint;
        for (int stage = 0; stage < stages; stage++) {
            int chunkSamples = chunkSamplesPerStage[stage];
            int chunkStart = pcm.length - chunkSamples;
            int repeats = repeatsPerStage[stage];

            for (int r = 0; r < repeats; r++) {
                System.arraycopy(pcm, chunkStart, output, writePos, chunkSamples);
                writePos += chunkSamples;
            }
        }

        return output;
    }

    /**
     * Splits the audio at a random point and pitch-shifts each half separately
     * (high then low)
     */
    private static short[] multiPitch(short[] pcm, float highFactor, float lowFactor) {
        if (highFactor <= 0 || lowFactor <= 0) throw new IllegalArgumentException("Pitch factors must be > 0");
        if (pcm.length < SAMPLE_RATE / 5) return pcm;

        Random random = new Random();
        int margin = pcm.length / 10;
        int splitPoint = margin + random.nextInt(Math.max(1, pcm.length - 2 * margin));

        short[] highSegment = Arrays.copyOfRange(pcm, 0, splitPoint);
        short[] lowSegment = Arrays.copyOfRange(pcm, splitPoint, pcm.length);

        short[] highPart = changePitch(highSegment, highFactor);
        short[] lowPart = changePitch(lowSegment, lowFactor);

        short[] output = new short[highPart.length + lowPart.length];
        System.arraycopy(highPart, 0, output, 0, highPart.length);
        System.arraycopy(lowPart, 0, output, highPart.length, lowPart.length);

        return output;
    }

}