clear all;

[filename,pathname]=uigetfile('*.*', 'Select the ECG Signal');
filewithpath=strcat(pathname,filename);
Fs=input('Enter Sampling Rate: ');

%ecg = load(filename);
x = readtable(filename); % load the ECG signal

% x = readtable('emg.csv'); % load the ECG signal
% 
% t = x.Var1;
% dt = mean(diff(t))

%Fs = 120  % Sampling Frequency

%Fs = 360

% Fnotch = 0.67; % Notch Frequency
% BW = 5; % Bandwidth
% Apass = 1; % Bandwidth Attenuation
% [b, a] = iirnotch (Fnotch/ (Fs/2), BW/(Fs/2), Apass);
% Hd = dfilt.df2 (b, a);

x1=x.Var2;
x2=x1./ max(x1);
subplot (3, 1, 1), plot(x2), title ('ECG Signal with baswline wander'), grid on

% y0=filter (Hd, x2);
% subplot (3, 1, 2), plot(y0), title ('ECG signal with low-frequency noise (baswline wander) Removed'), grid on

Fnotch = 50; % Notch Frequency
BW = 49; % Bandwidth
Apass = 1; % Bandwidth Attenuation

[b, a] = iirnotch (Fnotch/(Fs/2), BW/(Fs/2), Apass);
Hd1 = dfilt.df2 (b, a);
y1=filter (Hd1, x2);
subplot (3, 1, 2), plot (y1), title ('ECG signal with power line noise Removed'), grid on

a1 = x.Var3;
a2 = x.Var4;
a3 = x.Var5;
y1 = x2;
[y1,d]=lowpass(x2,1,Fs);
y1 = y1/max (y1);
%a = a1 
a = a1 + a2 + a3
a = a/max(a)
% a1 = a1/max(a1)

Mu= 0.04;
Hd = dsp.LMSFilter('Length',32,'Method','Normalized LMS','StepSize', Mu)
[y,e,w] = Hd(a,y1);
% [y,e,w] = Hd(a2,e);
% [y,e,w] = Hd(a3,e);

subplot (3, 1, 3); plot (e), title ('Noise (motion artifact) estimate'), grid on
a = a1/max(a1) + a2/max(a2) + a3/max(a3)

% yy = smooth(e,10)


[y,d]=lowpass(x2,1,Fs);
% y = smooth(y,20)
subplot (3, 1, 2); plot (y), title ('Adaptively filtered/ Noise free ECG signal'), grid on
%subplot (3, 1, 3); plot (x.Var1,e), title ('Adaptively filtered/ Noise free ECG signal'), grid on

% l =32;
% Mu= 0.0008;
% m = 2;
% lms = dsp.LMSFilter('Length',l,'StepSize',Mu);
% 
% [mmse,emse,meanW,mse,traceK] = msepred(lms,a,y1,m);
% [simmse,meanWsim,Wsim,traceKsim] = msesim(lms,a,y1,m);
% 
% nn = m:m:size(a,1);
% semilogy(nn,simmse,[0 size(a,1)],[(emse+mmse)...
%     (emse+mmse)],nn,mse,[0 size(a,1)],[mmse mmse])
% title('Mean Squared Error Performance')
% axis([0 size(a,1) 0.001 10])
% legend('MSE (Sim.)','Final MSE','MSE','Min. MSE')
% xlabel('Time Index')
% ylabel('Squared Error Value')
% dt=diff(t)'
% plot([dt(1:6650),dt(6700:10000)])