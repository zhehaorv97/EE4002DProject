clear;
close all;
clc;

f2 = 1;
fs = 50000;
dur = 3.0;

n = fs * dur;
t = (1:n) / fs;
s = sin(2*pi*f2*t);
s = s./max(s);
subplot (2, 1, 1), plot (s), title ('ECG signal with power line noise Removed'), grid on


ecg = readtable('data8.csv').Var2;
ecg = ecg.';
ecg = ecg./max(ecg);
subplot (2, 1, 2), plot (ecg), title ('ECG signal with power line noise Removed'), grid on

sound(ecg, 5000);
pause(dur + 0.5);
audiowrite("ecg.wav",ecg, 512);

% t=linspace(0,1,44100);
% f=400;
% yleft=sin(t*2*pi*f);
% f=600;
% yright=sin(t*2*pi*f);
% y=zeros(44100*2,2);
% y(1:length(yleft),1)=yleft;
% y(length(yleft)+(1:length(yright)),2)=yright;
% % For visualization
% stackedplot(y,"DisplayLabels",["Left" "Right"])
% sound(y,44100)

