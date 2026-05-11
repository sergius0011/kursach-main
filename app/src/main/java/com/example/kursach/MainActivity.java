package com.example.kursach;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView statusTextView;
    private TextView modeTextView;
    private TextView connectionStatusTextView;
    private TextView modeDescriptionTextView;
    private ImageButton btnModeDown, btnModeUp;
    private ToggleButton toggleConnection;
    private Button btnOpenLog;
    private Button btnTestAlarm;
    private Button btnSilenceAlarm;

    private View logContainer;
    private RecyclerView logRecyclerView;
    private LogAdapter logAdapter;
    private List<String> logList = new ArrayList<>();
    private boolean logVisible = false;

    private static final String CHANNEL_ID = "motion_detection_channel";
    private static final String CHANNEL_NAME = "Обнаружение движения";
    private static final String CHANNEL_DESCRIPTION = "Уведомления о движении в зоне контроля";
    private static final int NOTIFICATION_ID = 1;
    private NotificationManager notificationManager;

    private Socket socket;
    private BufferedWriter writer;
    private BufferedReader reader;
    private boolean isConnected = false;
    private String arduinoIP = "192.168.4.1";
    private int arduinoPort = 80;
    private int currentMode = 1;
    private String[] modeDescriptions = {
            "Недолго, медленно",
            "Недолго, быстро",
            "Долго, быстро",
            "Долго, медленно",
            "Недолго, очень быстро"
    };
    private int notificationId = 1;
    private static final int NOTIFICATION_PERMISSION_CODE = 100;

    private Handler handler = new Handler();
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private ReadThread readThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupEventListeners();
        updateModeDisplay();
        createNotificationChannel();
        askNotificationPermission();

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        handler.postDelayed(connectionCheckRunnable, 500);
    }

    private void initViews() {
        statusTextView = findViewById(R.id.statusTextView);
        modeTextView = findViewById(R.id.modeTextView);
        modeDescriptionTextView = findViewById(R.id.modeDescriptionTextView);
        connectionStatusTextView = findViewById(R.id.connectionStatusTextView);

        toggleConnection = findViewById(R.id.toggleConnection);
        btnTestAlarm = findViewById(R.id.btnTestAlarm);
        btnSilenceAlarm = findViewById(R.id.btnSilenceAlarm);

        btnModeDown = findViewById(R.id.btnModeLeft);
        btnModeUp = findViewById(R.id.btnModeRight);

        logContainer = findViewById(R.id.logContainer);
        logRecyclerView = findViewById(R.id.logRecyclerView);

        logAdapter = new LogAdapter(logList);
        logRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        logRecyclerView.setAdapter(logAdapter);
    }

    private void setupEventListeners() {
        toggleConnection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                new ConnectTask().execute();
            } else {
                disconnect();
            }
        });

        btnModeDown.setOnClickListener(v -> {
            if (currentMode > 1) {
                setMode(currentMode - 1);
            } else {
                setMode(5);
            }
        });

        btnModeUp.setOnClickListener(v -> {
            if (currentMode < 5) {
                setMode(currentMode + 1);
            } else {
                setMode(1);
            }
        });

        btnTestAlarm.setOnClickListener(v -> simulateMotionDetection());

        btnSilenceAlarm.setOnClickListener(v -> {
            sendCommandToArduino("SILENCE");
            Toast.makeText(this, "Сигнал отключен", Toast.LENGTH_SHORT).show();
            addToLog("Звук отключен");
        });

        Button btnClearLog = findViewById(R.id.btnClearLog);
        btnClearLog.setOnClickListener(v -> clearLog());
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Разрешение получено", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Уведомления не будут работать", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void addToLog(String message) {
        runOnUiThread(() -> {
            String time = timeFormat.format(new Date());
            String logEntry = time + " - " + message;
            logList.add(logEntry);

            if (logList.size() > 50) {
                logList.remove(0);
            }

            logAdapter.notifyDataSetChanged();
            if (logList.size() > 0) {
                logRecyclerView.smoothScrollToPosition(logList.size() - 1);
            }
        });
    }

    private void clearLog() {
        if (logList.size() > 0) {
            logList.clear();
            logAdapter.notifyDataSetChanged();
            Toast.makeText(this, "Лог очищен", Toast.LENGTH_SHORT).show();
            addToLog("Лог очищен");
        } else {
            Toast.makeText(this, "Лог уже пуст", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendModeToArduino(int mode) {
        String command = String.valueOf(mode);
        sendCommandToArduino(command);
        addToLog("Отправлено на Arduino: режим " + mode);
    }

    private void sendCommandToArduino(String command) {
        if (isConnected && writer != null) {
            new Thread(() -> {
                try {
                    writer.write(command + "\n");
                    writer.flush();
                    Log.d("SendCommand", "Отправлено на Arduino: " + command);
                } catch (IOException e) {
                    Log.e("SendCommand", "Ошибка отправки: " + e.getMessage());
                    runOnUiThread(() -> addToLog("Ошибка отправки команды: " + command));
                }
            }).start();
        } else {
            addToLog("Нет подключения для отправки команды: " + command);
        }
    }

    private void setMode(int mode) {
        currentMode = mode;
        runOnUiThread(() -> {
            modeTextView.setText("Режим " + mode);
            modeDescriptionTextView.setText(modeDescriptions[mode - 1]);
        });
        sendModeToArduino(mode);
        addToLog("Установлен режим " + mode);
    }

    private void processReceivedData(String data) {
        runOnUiThread(() -> {
            addToLog("Получено: " + data);

            if (data.contains("MOTION")) {
                statusTextView.setText("Обнаружено движение!");
                statusTextView.setTextColor(0xFFF44336);
                addToLog("Обнаружено движение!");
                sendMotionNotification("Обнаружено движение в зоне контроля!");
                handler.postDelayed(() -> {
                    statusTextView.setText("Нет движения");
                    statusTextView.setTextColor(getResources().getColor(R.color.green));
                }, 5000);
            }
            else if (data.startsWith("MODE:")) {
                try {
                    int receivedMode = Integer.parseInt(data.substring(5));
                    if (receivedMode >= 1 && receivedMode <= 5) {
                        currentMode = receivedMode;
                        modeTextView.setText("Режим " + currentMode);
                        modeDescriptionTextView.setText(modeDescriptions[currentMode - 1]);
                        addToLog("Подтверждение режима " + receivedMode + " от Arduino");
                    }
                } catch (NumberFormatException e) {
                    Log.e("ProcessData", "Ошибка парсинга режима: " + data);
                }
            }
            else if (data.startsWith("ALARM:")) {
                String alarmState = data.substring(6);
                addToLog("Состояние сигнализации: " + alarmState);
            }
            else if (data.equals("CONNECTED")) {
                addToLog("Подключение подтверждено");
                // После подтверждения отправляем текущий режим
                sendModeToArduino(currentMode);
            }
            else if (data.matches("\\d+")) {
                try {
                    int receivedMode = Integer.parseInt(data);
                    if (receivedMode >= 1 && receivedMode <= 5) {
                        currentMode = receivedMode;
                        modeTextView.setText("Режим " + currentMode);
                        modeDescriptionTextView.setText(modeDescriptions[currentMode - 1]);
                        addToLog("Подтверждение режима " + receivedMode + " от Arduino");
                    }
                } catch (NumberFormatException e) {
                    Log.e("ProcessData", "Ошибка парсинга: " + data);
                }
            }
            else {
                addToLog("Получено от Arduino: " + data);
            }
        });
    }

    private void updateModeDisplay() {
        modeTextView.setText("Режим " + currentMode);
        modeDescriptionTextView.setText(modeDescriptions[currentMode - 1]);
    }

    private void simulateMotionDetection() {
        statusTextView.setText("Обнаружено движение!");
        statusTextView.setTextColor(getResources().getColor(R.color.red));
        addToLog("Обнаружено движение (тест)");
        sendMotionNotification("Датчик обнаружил движение!");
        sendCommandToArduino("TEST");

        handler.postDelayed(() -> {
            statusTextView.setText("Статус: Нет движения");
            statusTextView.setTextColor(getResources().getColor(R.color.green));
        }, 1000);
    }

    private void sendMotionNotification(String message) {
        int currentNotificationId = notificationId++;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("🚨 ОБНАРУЖЕНО ДВИЖЕНИЕ 🚨")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        builder.setDefaults(NotificationCompat.DEFAULT_ALL);

        try {
            notificationManager.notify(currentNotificationId, builder.build());
            addToLog("Отправлено уведомление: " + message);
        } catch (SecurityException e) {
            addToLog("Ошибка отправки уведомления: " + e.getMessage());
        }
    }

    private final Runnable connectionCheckRunnable = new Runnable() {
        @Override
        public void run() {
            runOnUiThread(() -> {
                if (isConnected) {
                    connectionStatusTextView.setText("Подключено");
                    connectionStatusTextView.setTextColor(getResources().getColor(R.color.green));
                } else {
                    connectionStatusTextView.setText("Отключено");
                    connectionStatusTextView.setTextColor(getResources().getColor(R.color.red));
                }
            });
            handler.postDelayed(this, 500);
        }
    };

    private class ConnectTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                socket = new Socket(arduinoIP, arduinoPort);
                socket.setSoTimeout(5000); // Таймаут 5 секунд
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                return true;
            } catch (IOException e) {
                Log.e("Connection", "Ошибка подключения: " + e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                isConnected = true;
                Toast.makeText(MainActivity.this, "Подключено к Arduino", Toast.LENGTH_SHORT).show();
                addToLog("Подключение установлено");

                readThread = new ReadThread();
                readThread.start();
            } else {
                isConnected = false;
                Toast.makeText(MainActivity.this, "Ошибка подключения", Toast.LENGTH_SHORT).show();
                toggleConnection.setChecked(false);
                addToLog("Ошибка подключения");
            }
        }
    }

    private class ReadThread extends Thread {
        private boolean running = true;

        @Override
        public void run() {
            try {
                while (running && isConnected && reader != null) {
                    if (reader.ready()) {
                        String line = reader.readLine();
                        if (line != null) {
                            processReceivedData(line);
                        }
                    } else {
                        Thread.sleep(100);
                    }
                }
            } catch (IOException e) {
                Log.e("ReadThread", "Ошибка чтения: " + e.getMessage());
                runOnUiThread(() -> {
                    isConnected = false;
                    toggleConnection.setChecked(false);
                    addToLog("Соединение разорвано: " + e.getMessage());
                });
            } catch (InterruptedException e) {
                Log.e("ReadThread", "Поток прерван");
            } finally {
                runOnUiThread(() -> {
                    if (toggleConnection.isChecked()) {
                        toggleConnection.setChecked(false);
                    }
                });
            }
        }

        public void stopReading() {
            running = false;
        }
    }

    private void disconnect() {
        isConnected = false;

        if (readThread != null) {
            readThread.stopReading();
            readThread = null;
        }

        try {
            if (writer != null) {
                writer.close();
                writer = null;
            }
            if (reader != null) {
                reader.close();
                reader = null;
            }
            if (socket != null) {
                socket.close();
                socket = null;
            }
            addToLog("Соединение разорвано");
        } catch (IOException e) {
            Log.e("Disconnect", "Ошибка отключения: " + e.getMessage());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnect();
        handler.removeCallbacks(connectionCheckRunnable);
    }

    private static class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {
        private List<String> logs;

        public LogAdapter(List<String> logs) {
            this.logs = logs;
        }

        @NonNull
        @Override
        public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_log, parent, false);
            return new LogViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
            holder.bind(logs.get(position));
        }

        @Override
        public int getItemCount() {
            return logs.size();
        }

        static class LogViewHolder extends RecyclerView.ViewHolder {
            private TextView logTextView;

            public LogViewHolder(@NonNull View itemView) {
                super(itemView);
                logTextView = itemView.findViewById(R.id.logTextView);
            }

            public void bind(String log) {
                logTextView.setText(log);
            }
        }
    }
}