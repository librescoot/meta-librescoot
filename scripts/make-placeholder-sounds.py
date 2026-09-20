#!/usr/bin/env python3
"""Generate the committed placeholder startup sounds for the boot themes.

The official layer ships only these clean-room chimes. A build server that is
allowed to carry the retail startup sounds overlays files/sounds/<theme>.wav
from a private layer via FILESEXTRAPATHS:prepend; nothing here changes for
that, and the placeholders keep an unmodified checkout booting.

boot-animation's audio path accepts only 48 kHz stereo 16-bit PCM WAV, so that
is what this writes. Run from the layer root, where the script sits:

    python3 scripts/make-placeholder-sounds.py

The output is deterministic: same script, same bytes.
"""
import wave
from pathlib import Path

import numpy as np

SR = 48000
OUT = (Path(__file__).resolve().parent.parent
       / 'recipes-graphics/boot-animation/files/sounds')

# A synthesizer chord in the neighbourhood of a well-known fruit-flavoured
# startup sound: F# major, nothing else in common with it.
COOPERTINO_CHORD = [185.00, 233.08, 277.18, 369.99, 554.37]  # F#3 A#3 C#4 F#4 C#5
# A rising G-major bell arpeggio for the XP-flavoured theme.
XP_ARPEGGIO = [392.00, 493.88, 587.33, 783.99, 987.77, 1174.66]


def envelope(n, attack, decay, tail=0.06):
    """Exponential decay with a raised-cosine attack and tail."""
    t = np.arange(n) / SR
    env = np.exp(-t / decay)
    a = min(n, max(1, int(attack * SR)))
    env[:a] *= 0.5 - 0.5 * np.cos(np.pi * np.arange(a) / a)
    r = min(n, max(1, int(tail * SR)))
    env[-r:] *= 0.5 + 0.5 * np.cos(np.pi * np.arange(r) / r)
    return env


def voice(freq, dur, *, decay, attack, detune_cents=3.0, bell=0.0,
          partials=((1.0, 1.0), (2.0, 0.45), (3.0, 0.2), (4.0, 0.09))):
    """One detuned, bell-touched note. Returns a mono float array."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    det = 2 ** (detune_cents / 1200.0)

    signal = np.zeros(n)
    weight = 0.0
    for ratio, amp in partials:
        # Two slightly detuned copies per partial: the slow beating is what
        # makes a stack of sines read as a synthesizer rather than a test tone.
        signal += amp * np.sin(2 * np.pi * freq * ratio * (1.0 / det) * t + ratio)
        signal += amp * np.sin(2 * np.pi * freq * ratio * det * t + ratio * 1.7)
        weight += 2 * amp
    signal /= weight

    if bell > 0.0:
        # Inharmonic strike partials for the metallic attack edge.
        strike = bell * np.exp(-t / 0.16)
        for ratio, amp in ((2.76, 1.0), (5.40, 0.45), (8.93, 0.2)):
            signal += amp * np.sin(2 * np.pi * freq * ratio * t) * strike

    return signal * envelope(n, attack, decay)


def mix(target, source, at):
    """Add source into target at sample offset at, growing target if needed."""
    end = at + len(source)
    if end > len(target):
        target.resize(end, refcheck=False)
    target[at:end] += source


def pad_to(*tracks):
    """Return tracks padded with silence to a common length."""
    length = max(len(track) for track in tracks)
    return tuple(np.pad(track, (0, length - len(track))) for track in tracks)


def stereo(mono, spread_ms=0.7):
    """Widen a mono signal without changing its length."""
    delay = int(spread_ms * SR / 1000)
    right = np.concatenate([np.zeros(delay), mono[:len(mono) - delay]])
    left = mono
    return np.stack([left, right * 0.995], axis=1)


def normalize(pcm, peak=0.9):
    high = float(np.max(np.abs(pcm)))
    if high > 0:
        pcm = pcm * (peak / high)
    # Soft-clip rather than hard-clip, in case a build-server file is hot.
    return np.tanh(pcm * 1.05) / np.tanh(1.05)


def coopertino():
    dur = 3.4
    length = int(dur * SR)
    left = np.zeros(length)
    right = np.zeros(length)
    for index, freq in enumerate(COOPERTINO_CHORD):
        # Wider detune and a longer ring for the lower notes keeps the chord
        # from turning to mud.
        detune = 2.2 + index * 0.5
        mix(left, voice(freq, dur, decay=1.35, attack=0.045, detune_cents=detune,
                        bell=0.16), int((0.02 * index) * SR))
        mix(right, voice(freq, dur, decay=1.35, attack=0.045, detune_cents=detune + 0.9,
                         bell=0.16), int((0.02 * index + 0.0009) * SR))
    left, right = pad_to(left, right)
    return normalize(np.stack([left, right], axis=1))


def windowsxp():
    dur = 2.6
    length = int(dur * SR)
    left = np.zeros(length)
    right = np.zeros(length)
    step = 0.135
    for index, freq in enumerate(XP_ARPEGGIO):
        note = voice(freq, dur, decay=0.95, attack=0.008,
                     detune_cents=2.0, bell=0.22)
        # Walk the arpeggio gently across the stereo field.
        pan = 0.5 - (index / (len(XP_ARPEGGIO) - 1)) * 0.5 if len(XP_ARPEGGIO) > 1 else 0.5
        at = int(index * step * SR)
        mix(left, note * (0.55 + 0.45 * pan), at)
        mix(right, note * (0.55 + 0.45 * (1.0 - pan)), int(at + 0.0006 * SR))
    # Low pad underneath so the arpeggio has a floor.
    pad = voice(196.00, dur, decay=2.4, attack=0.25, detune_cents=4.0, bell=0.0,
                partials=((1.0, 1.0), (2.0, 0.3), (3.0, 0.12)))
    mix(left, pad * 0.35, 0)
    mix(right, pad * 0.35, int(0.0012 * SR))
    left, right = pad_to(left, right)
    return normalize(np.stack([left, right], axis=1))


def reversed_track(pcm, fade_ms=6):
    out = pcm[::-1].copy()
    fade = min(len(out), max(1, int(fade_ms * SR / 1000)))
    out[:fade] *= np.linspace(0.0, 1.0, fade)[:, None]
    out[-fade:] *= np.linspace(1.0, 0.0, fade)[:, None]
    return out


def write_wav(path, pcm):
    data = np.clip(pcm, -1.0, 1.0)
    frames = (data * 32767.0).astype('<i2')
    with wave.open(str(path), 'wb') as handle:
        handle.setnchannels(2)
        handle.setsampwidth(2)
        handle.setframerate(SR)
        handle.writeframes(frames.tobytes())
    print(f'{path.relative_to(OUT.parent.parent.parent)}: '
          f'{len(frames) / SR:.2f}s, {path.stat().st_size} bytes')


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    xp = windowsxp()
    write_wav(OUT / 'windowsxp.wav', xp)
    write_wav(OUT / 'librescoot-xp.wav', reversed_track(xp))
    write_wav(OUT / 'coopertino.wav', coopertino())


if __name__ == '__main__':
    main()
