import torch
from torch.utils.data import DataLoader, Dataset
from sklearn.metrics import accuracy_score, precision_score, f1_score, recall_score, classification_report
from scipy.signal import cheby2, filtfilt, iirnotch

import matplotlib.pyplot as plt
import pandas as pd
import scipy.signal as signal
from ecgdetectors import Detectors
import numpy as np
import pickle
import torch.nn as nn
import os

path = 'c:\\Users\\zhehao\\Desktop\\ECG Server\\'
os.chdir(path)
beatsIndex=[]

def cheby2_bandpass_filter(data, lowcut, highcut, fs, order=5, rs=30):
    nyq = 0.5 * fs
    low = lowcut / nyq
    high = highcut / nyq
    b, a = cheby2(order, rs, [low, high], btype='band')
    y = filtfilt(b, a, data)
    #y = data
    return y


def notch_filter(data, notch_freq, q, fs):
    nyq = 0.5 * fs
    freq = notch_freq / nyq
    b, a = iirnotch(freq, q)
    y = filtfilt(b, a, data)
    return y

import scipy.signal as signal

def process_ecg_data(ecg_signal, fs, target_fs):
    if fs != target_fs:
        ecg_signal = signal.resample(ecg_signal, int(ecg_signal.size * target_fs / fs))
    filtered_ecg_signal = cheby2_bandpass_filter(ecg_signal, 5, 50, target_fs, order=6, rs=30)
    filtered_ecg_signal = notch_filter(filtered_ecg_signal, 50, 30, target_fs)
    return filtered_ecg_signal


def extract_beats(ecg_data, fs, target_fs, window_size=280):
    processed_ecg_data = process_ecg_data(ecg_data, fs, target_fs)
    detectors = Detectors(target_fs)
    r_peaks = detectors.wqrs_detector(processed_ecg_data)

    beat_locations = []
    global beatsIndex 
    beatsIndex = r_peaks
    half_window = window_size // 2
    for i, r_peak in enumerate(r_peaks):
        print(r_peak- half_window)
        if r_peak - half_window >= 0 and r_peak + half_window < len(processed_ecg_data):
            beat_location = processed_ecg_data[r_peak - half_window:r_peak + half_window]
            beat_locations.append(beat_location)

    return np.array(beat_locations)

fs = 512
target_fs = 360
window_size = 280

# Read the CSV file
#df = pd.read_csv("3leadnoMC.csv")
#df = pd.read_csv("3leadwMC.csv")
#df = pd.read_csv("posnegnoMC.csv")
df = pd.read_csv("3leadnoMC.csv")
#df = pd.read_csv("data1.csv")
#Extract the ECG data from the first column
ecg_data = df.iloc[:, 0].values

# Process the ECG data
processed_ecg_data = process_ecg_data(ecg_data, fs, target_fs)

# Extract beats and annotations
beats = extract_beats(processed_ecg_data, fs, target_fs, window_size)

# Load the saved StandardScaler object from the file
with open('scaler.pkl', 'rb') as file:
    scaler = pickle.load(file)

# Load the saved label_encoder object from the file
with open('label_encoder.pkl', 'rb') as file:
    label_encoder = pickle.load(file)
   
    
    
# Scale and reshape the beats
scaled_beats = scaler.transform(beats)
X = torch.tensor(scaled_beats, dtype=torch.float32).unsqueeze(1)

# Predict the classes
# Load the model

class EcgModel(nn.Module):
    def __init__(self, input_size, hidden_size, num_layers, num_classes):
        super(EcgModel, self).__init__()
        self.hidden_size = hidden_size
        self.num_layers = num_layers
        self.lstm = nn.LSTM(input_size, hidden_size, num_layers, batch_first=True)
        self.fc = nn.Linear(hidden_size, num_classes)

    def forward(self, x):
        h0 = torch.zeros(self.num_layers, x.size(0), self.hidden_size).to(device)
        c0 = torch.zeros(self.num_layers, x.size(0), self.hidden_size).to(device)

        out, _ = self.lstm(x, (h0, c0))
        out = self.fc(out[:, -1, :])
        return out

    
input_size = 280
hidden_size = 64
num_layers = 2
num_classes = 5
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

model = EcgModel(input_size, hidden_size, num_layers, num_classes).to(device)

model.load_state_dict(torch.load("finale_model.pt"))
model.to(device)
model.eval()


df = pd.read_csv("3leadnoMC.csv")


#df = pd.read_csv("data1.csv")
#Extract the ECG data from the first column
ecg_data = df.iloc[:, 1].values

# Process the ECG data
processed_ecg_data = process_ecg_data(ecg_data, fs, target_fs)

# Extract beats and annotations
beats = extract_beats(processed_ecg_data, fs, target_fs, window_size)

# Scale and reshape the beats
scaled_beats = scaler.transform(beats)
X = torch.tensor(scaled_beats, dtype=torch.float32).unsqueeze(1)


with torch.no_grad():
    outputs = model(X.to(device))
    _, preds = torch.max(outputs, 1)
    classes = label_encoder.inverse_transform(preds.cpu().numpy())


# Create a DataFrame with the results
results_df = pd.DataFrame({"Beat": range(1, len(classes) + 1), "Class": classes})

print(results_df)



from flask import Flask
from flask import request, render_template, redirect, url_for
import matlab.engine
import csv
# Importing the module
import os

# Getting the current working directory
cwd = os.getcwd()
files = os.listdir(path)

# for file in files:
#     print(file)
# Printing the current working directory
print("Th Current working directory is: {0}".format(cwd))

matlabEngine = matlab.engine.start_matlab()
matlabEngine.addpath(r'C:\Users\zhehao\Desktop\ECG Server',nargout=0)

fileNum = 0
for file in files:
    if (file[:4] == "data"):
        fileNum+=1

fileName = "data"+str(fileNum)+".csv"


df = pd.read_csv(fileName)
ecg_data = df.iloc[:, 1].values
# Process the ECG data
processed_ecg_data = process_ecg_data(ecg_data, fs, target_fs)
# Extract beats and annotations
beats = extract_beats(processed_ecg_data, fs, target_fs, window_size)
# Scale and reshape the beats
scaled_beats = scaler.transform(beats)
X = torch.tensor(scaled_beats, dtype=torch.float32).unsqueeze(1)

with torch.no_grad():
    outputs = model(X.to(device))
    _, preds = torch.max(outputs, 1)
    classes = label_encoder.inverse_transform(preds.cpu().numpy())

# Create a DataFrame with the results
results_df = pd.DataFrame({"Beat": range(1, len(classes) + 1), "Class": classes})
print(results_df["Class"])

def writedata(data, index):
    fileNum = 0
    files = os.listdir(path)
    for file in files:
        if (file[:4] == "data"):
            fileNum+=1
    files = os.listdir(path)
    data=data[1:-1]
    data=data.split(", ")

    print(data)
    fileNum+=1
    f = open("data"+str(fileNum)+".csv", 'a', newline="")
    writer = csv.writer(f)

    index = int(index)
    if (len(data)>=10240): 
        for i in range (index,10240):
            writer.writerow([i-index+1,int(data[i][:-2])])
        for i in range (0,index):
            writer.writerow([10240-index+i+1,int(data[i][:-2]),])
    
    else:
        for i in range (0,len(data)):
            writer.writerow([i+1,int(data[i][:-2])])

    f.close()

app = Flask(__name__)

@app.route("/")
def home():
    return render_template("home.html")

@app.route("/post", methods=["POST"])
def postF():
    value=request.form['value']
    value2=request.form['value2']
    #return (redirect(url_for("user",name=value)))
    #print(len(value.split(", ")),value2)
    writedata(value,value2)
    return ("done")

@app.route("/graph", methods=["POST"])
def postGraph():
    r_peaksMatArray=[]
    r_peaksMatArray2=[]
    r_peaksMatArray3=[]
    fileNum = 0
    files = os.listdir(path)
    for file in files:
        if (file[:4] == "data"):
            fileNum+=1
    fileName = "data"+str(fileNum)+".csv"
    with open(fileName, newline='') as csvfile:
        data = list(csv.reader(csvfile))
        labels = [row[0] for row in data]
        values = [row[1] for row in data]

    return render_template("index.html",  labels = labels, values = values, valuesR = [], Rpeaks = r_peaksMatArray, Rpeaks2 = r_peaksMatArray2, Rpeaks3 = r_peaksMatArray3, status = "Graph Displayed", route = "postGraph")


@app.route("/postRpeaks", methods=["POST"])
def postRpeaks():
    fileNum = 0
    files = os.listdir(path)
    for file in files:
        if (file[:4] == "data"):
            fileNum+=1

    fileName = "data"+str(fileNum)+".csv"
    with open(fileName, newline='') as csvfile:
        data = list(csv.reader(csvfile))
        labels = [row[0] for row in data]
        values = [row[1] for row in data]

    df = pd.read_csv(fileName)
    ecg_data = df.iloc[:, 1].values
    # Process the ECG data
    processed_ecg_data = process_ecg_data(ecg_data, fs, target_fs)
    # Extract beats and annotations
    beats = extract_beats(processed_ecg_data, fs, target_fs, window_size)
    # Scale and reshape the beats
    r_peaksMat = matlabEngine.R_peaks_detection(fileName).tomemoryview()
    r_peaksMat = r_peaksMat.tolist()
    print(r_peaksMat)

    r_peaksMatArray=[]
    r_peaksMatArray2=[]
    r_peaksMatArray3=[]

    for element in r_peaksMat[0]:
        if (element < 3414):
            r_peaksMatArray.append([str(element),])
        elif (element < 6828):
            r_peaksMatArray2.append([str(element-3414),])
        elif (element < 10240):
            r_peaksMatArray3.append([str(element-6828),])
    # for i, index in enumerate(beatsIndex):
    #     index = index*512/360*9993/7009
    #     if (index< 3414):
    #         r_peaksMatArray.append([str(index),])
            
    #     if (3414 <= index < 6828):
    #         r_peaksMatArray2.append([str(index-3414),])

    #     if (6828 <= index < 10240):
    #         r_peaksMatArray3.append([str(index-6828),])


    print("rpeaks are",r_peaksMatArray)
    with open('M.csv', newline='') as csvfile:
     dataR = list(csv.reader(csvfile))
    valuesR = [row[0] for row in dataR]
    return render_template("index.html",  labels = labels, values = values, valuesR = valuesR, Rpeaks = r_peaksMatArray, Rpeaks2 = r_peaksMatArray2, Rpeaks3 = r_peaksMatArray3, status = "Wavelet Transform Done!", route = "postRpeaks")





@app.route("/postMLResult", methods=["POST"])
def postMLResult():
    fileNum = 0
    files = os.listdir(path)
    for file in files:
        if (file[:4] == "data"):
            fileNum+=1

    fileName = "data"+str(fileNum)+".csv"
    with open(fileName, newline='') as csvfile:
        data = list(csv.reader(csvfile))
        labels = [row[0] for row in data]
        values = [row[1] for row in data]
    df = pd.read_csv(fileName)
    ecg_data = df.iloc[:, 1].values
    # Process the ECG data
    processed_ecg_data = process_ecg_data(ecg_data, fs, target_fs)
    # Extract beats and annotations
    beats = extract_beats(processed_ecg_data, fs, target_fs, window_size)
    # Scale and reshape the beats
    scaled_beats = scaler.transform(beats)
    X = torch.tensor(scaled_beats, dtype=torch.float32).unsqueeze(1)
    with torch.no_grad():
        outputs = model(X.to(device))
        _, preds = torch.max(outputs, 1)
        classes = label_encoder.inverse_transform(preds.cpu().numpy())
    results_df = pd.DataFrame({"Beat": range(1, len(classes) + 1), "Class": classes})
    print(results_df["Class"][0])



    #processed_ecg_data = process_ecg_data(ecg_data, fs, target_fs)
    # r_peaks = Detectors(target_fs).two_average_detector(processed_ecg_data)
    status = results_df["Class"]
    # print(r_peaks)

    r_peaksMatArray=[]
    r_peaksMatArray2=[]
    r_peaksMatArray3=[]
    for i, index in enumerate(beatsIndex):
        half_window = window_size/2
        index = index*512/360*9970/7009
        if index - half_window >= 0 and index + half_window < 10000:
            if (index+half_window< 3414):
                r_peaksMatArray.append([str(index+half_window),])
            if (index-half_window< 3414):
                r_peaksMatArray.append([str(index-half_window),])
                
            if (3414 <= index+half_window < 6828):
                r_peaksMatArray2.append([str(index+half_window-3414),])
            if (3414 <= index-half_window < 6828):
                r_peaksMatArray2.append([str(index-half_window-3414),])

            if (6828 <= index+half_window < 10240):
                r_peaksMatArray3.append([str(index+half_window-6828),])
            if (6828 <= index-half_window < 10240):
                r_peaksMatArray3.append([str(index-half_window-6828),])
            
        # if r_peak - half_window >= 0 and r_peak + half_window < len(processed_ecg_data):
        
        #     r_peak = r_peak*12/360*9970/7009
            
        #     if (r_peak+half_window < 3414):
        #         r_peaksMatArray.append([str(r_peak+half_window),])
        #     if (r_peak-half_window < 3414):
        #         r_peaksMatArray.append([str(r_peak-half_window),])

        #     if (3414 <= r_peak+half_window < 6828):
        #         r_peaksMatArray2.append([str(r_peak+half_window-3414),])
        #     if (3414 <= r_peak-half_window < 6828):
        #         r_peaksMatArray2.append([str(r_peak-half_window-3414),])

        #     if (6828 <= r_peak+half_window < 10240):
        #         r_peaksMatArray3.append([str(r_peak+half_window-6828),])
        #     if (6828 <= r_peak-half_window < 10240):
        #         r_peaksMatArray3.append([str(r_peak-half_window-6828),])

    #values = [row[1] for row in data]
    anomaliesDetected = False
    statusR = str(status)[:-26]
    statusE =""
    status = str(status)[:-26].split("\n")
    for classification in status:
        if len(classification) > 6:
            if classification[-1] != "N":
                statusE += classification[:-5] + ' '
                anomaliesDetected = True

    if(anomaliesDetected):
        status = statusR
        status2 = "Anomalies detected at: reading " + statusE
    else:
        status = statusR
        status2 = "No anomaly detected"
    return render_template("index.html",  labels = labels, values = values,Rpeaks = r_peaksMatArray, Rpeaks2 = r_peaksMatArray2, Rpeaks3 = r_peaksMatArray3, status = status, status2 = status2, route = "postMLResult")

if __name__ == "__main__":
    app.run(host="0.0.0.0", port ="5000", debug = True)