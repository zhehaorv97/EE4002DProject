clear all;

[filename,pathname]=uigetfile('*.*', 'Select the ECG Signal');
filewithpath=strcat(pathname,filename);

x = readtable(filename) % load the ECG signal
x1=x.Var2;
x2=x1./ max(x1);
subplot (4, 1, 1), plot(x2), title ('Raw ECG'), grid on

a1 = x.Var3;
a2 = x.Var4;
a3 = x.Var5;
y1 = x2;
a = a1 + a2 + a3
a = a/max(a)

Mu= 0.04;
Hd = dsp.LMSFilter('Length',32,'Method','Normalized LMS','StepSize', Mu)
[y,e,w] = Hd(a,y1);
subplot (4, 1, 2); plot (y), title ('Noise (motion artifact) estimate'), grid on
a = a1/max(a1) + a2/max(a2) + a3/max(a3)
subplot (4, 1, 3); plot (e), title ('Adaptively filtered ECG signal'), grid on
writematrix(e,'data1e.csv')

Fnotch = 50; % Notch Frequency
BW = 49; % Bandwidth
Apass = 1; % Bandwidth Attenuation
Fs=512;
[b, a] = iirnotch(Fnotch/(Fs/2), BW/(Fs/2), Apass);
Hd1 = dfilt.df2 (b, a);
y1=filter (Hd1, e);
[y1,d]=lowpass(y1,1,100);
subplot (4, 1, 4); plot (y1), title ('Adaptively filtered + Notch Filtered + Low-pass Filtered ECG Signal'), grid on