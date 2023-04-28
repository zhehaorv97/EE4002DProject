

%Ys = Rpeaks_detection();


    
[filename,pathname]=uigetfile('*.*', 'Select the ECG Signal');
%filewithpath=strcat("data1",filename);
Fs=input('Enter Sampling Rate: ');


%ecg = load(filename);
x = readtable(filename); % load the ECG signal
ecg = x;
%ecg = rows2vars(x);
ecgsig = (ecg.Var1)./200;
t=1:length(ecgsig);
tx=t./Fs;

wt = modwt (ecgsig, 4, 'sym4');
wtrec = zeros(size(wt));
wtrec(3:4,:) = wt(3:4,:);
y = imodwt(wtrec, 'sym4');

y = abs(y).^2;
avg=mean(y);
[Rpeaks,locs] = findpeaks(y,t,'MinPeakHeight',8*avg, 'MinPeakDistance',50)

nohb=length(locs);
timelimit=length(ecgsig)/Fs;
hbpermin=(nohb*60)/timelimit;
disp(strcat('Heart Rate=', num2str(hbpermin)))

subplot(211)
plot(tx,ecgsig);
xlim([0,timelimit]);
grid on;
xlabel('Seconds')
title('ECG Signal For Positive and Negative Electrodes After Adaptive Filtering')

subplot(212)
plot(t,y)
grid on;
xlim([0,length(ecgsig)]);
hold on
plot(locs,Rpeaks,'ro')
xlabel('Samples')
title(strcat('R Peaks found and Heart Rate: ',num2str(hbpermin)))
%y.' %code to transpose a row matrix to column matrix
writematrix(y.','M.csv') % y is a matrix the after wavelet transform data but is 1xlength now
peaklocations = int64(locs);

