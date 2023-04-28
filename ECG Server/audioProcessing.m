[audioIn,fs] = audioread("ecg.wav");

audioOut = stretchAudio(audioIn,0.5);
audioOut = stretchAudio(audioOut,0.5);
audioOut = stretchAudio(audioOut,0.5);
audioOut = stretchAudio(audioOut,0.8);