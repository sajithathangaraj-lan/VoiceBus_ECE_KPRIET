package com.example.smartbus;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String TAG = "MainActivity";

    // Text to Speech
    private TextToSpeech mTTS;
    private boolean mIsTTSInitialized = false;

    // Gesture Detection
    private GestureDetector mGestureDetector;
    private View mWelcomeOverlay;
    private View mMainRoot;

    // State Variables
    private boolean mIsDashboardActive = false;
    private Dialog mCurrentDialog = null;

    // Vibration patterns
    private static final long SHORT_VIB = 100;
    private static final long MEDIUM_VIB = 300;
    private static final long LONG_VIB = 600;
    private final long[] BOARD_CONFIRM_VIB = {0, 150, 100, 150, 100, 400};
    private final long[] SKIP_CONFIRM_VIB = {0, 100, 80, 100, 80, 100};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI Elements
        mMainRoot = findViewById(R.id.main_root);
        mWelcomeOverlay = findViewById(R.id.welcome_overlay);

        // Initialize TextToSpeech
        mTTS = new TextToSpeech(this, this);

        // Initialize Gesture Detector for main screen
        mGestureDetector = new GestureDetector(this, new MainGestureListener());

        // Attach touch listener to main screen and welcome overlay to capture gestures anywhere
        View.OnTouchListener touchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }
        };

        mMainRoot.setOnTouchListener(touchListener);
        mWelcomeOverlay.setOnTouchListener(touchListener);

        // TalkBack click listener as safety fallback
        mWelcomeOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If TalkBack or single click is used
                speakInstructions();
            }
        });
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = mTTS.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language not supported");
            } else {
                mIsTTSInitialized = true;
                // Speak the welcome greeting when initialized
                speak(getString(R.string.tts_welcome));
            }
        } else {
            Log.e(TAG, "TextToSpeech Initialization Failed");
        }
    }

    /**
     * Speaks the given text using Text-to-Speech.
     */
    private void speak(String text) {
        if (mIsTTSInitialized && mTTS != null) {
            mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SmartBusSpeechID");
        } else {
            Log.w(TAG, "TTS not initialized yet. Message: " + text);
        }
    }

    /**
     * Helper to speak the instructions based on current state.
     */
    private void speakInstructions() {
        if (!mIsDashboardActive) {
            speak(getString(R.string.tts_welcome));
        } else if (mCurrentDialog == null) {
            speak(getString(R.string.tts_dashboard_loaded));
        }
    }

    /**
     * Triggers vibration for a given duration.
     */
    private void triggerVibration(long durationMs) {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(durationMs);
            }
        }
    }

    /**
     * Triggers vibration using a specific pattern.
     */
    private void triggerVibrationPattern(long[] pattern) {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                vibrator.vibrate(pattern, -1);
            }
        }
    }

    /**
     * Activates the dashboard, hiding the welcome overlay.
     */
    private void activateDashboard() {
        mIsDashboardActive = true;
        mWelcomeOverlay.setVisibility(View.GONE);
        triggerVibration(MEDIUM_VIB);
        speak(getString(R.string.tts_dashboard_loaded));
    }

    /**
     * Opens the Confirmation Dialog (handles both Boarding and Skipping).
     */
    private void showConfirmationDialog(final boolean isBoarding) {
        if (mCurrentDialog != null && mCurrentDialog.isShowing()) {
            mCurrentDialog.dismiss();
        }

        mCurrentDialog = new Dialog(this);
        mCurrentDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mCurrentDialog.setContentView(R.layout.dialog_feedback);
        
        // Make the dialog background transparent so the rounded card is visible
        if (mCurrentDialog.getWindow() != null) {
            mCurrentDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            // Force it to fill width
            mCurrentDialog.getWindow().setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        // Get dialog views
        TextView titleText = mCurrentDialog.findViewById(R.id.dialog_title);
        TextView messageText = mCurrentDialog.findViewById(R.id.dialog_message);
        TextView iconText = mCurrentDialog.findViewById(R.id.dialog_icon);
        Button btnConfirm = mCurrentDialog.findViewById(R.id.dialog_btn_confirm);
        Button btnCancel = mCurrentDialog.findViewById(R.id.dialog_btn_cancel);

        // Customize based on Boarding vs Skipping
        if (isBoarding) {
            iconText.setText("🚌");
            titleText.setText(R.string.dialog_board_title);
            titleText.setTextColor(getResources().getColor(R.color.green_board));
            messageText.setText(R.string.dialog_board_msg);
            btnConfirm.setText(R.string.dialog_board_confirm);
            btnConfirm.setBackgroundResource(R.drawable.button_board_background);
            speak(getString(R.string.tts_board_dialog_opened));
        } else {
            iconText.setText("⏭️");
            titleText.setText(R.string.dialog_skip_title);
            titleText.setTextColor(getResources().getColor(R.color.gold_primary));
            messageText.setText(R.string.dialog_skip_msg);
            btnConfirm.setText(R.string.dialog_skip_confirm);
            btnConfirm.setBackgroundResource(R.drawable.button_skip_background);
            speak(getString(R.string.tts_skip_dialog_opened));
        }

        // Action: Confirm Action (can be clicked directly or triggered via dialog gesture)
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmAction(isBoarding);
            }
        });

        // Action: Cancel Dialog (can be clicked directly or triggered via dialog gesture)
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelAction();
            }
        });

        // Add gestural shortcut to Dialog overlay (Double-Tap to Confirm, Long-Press to Cancel)
        final GestureDetector dialogGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                confirmAction(isBoarding);
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                cancelAction();
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                // Repeat dialog audio information on tap
                if (isBoarding) {
                    speak(getString(R.string.tts_board_dialog_opened));
                } else {
                    speak(getString(R.string.tts_skip_dialog_opened));
                }
                return true;
            }
        });

        // Listen for gestures anywhere on the dialog content view
        View dialogRoot = mCurrentDialog.findViewById(R.id.dialog_icon).getParent();
        if (dialogRoot != null) {
            dialogRoot.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return dialogGestureDetector.onTouchEvent(event);
                }
            });
        }

        mCurrentDialog.show();
    }

    /**
     * Executes confirmation logic for Boarding or Skipping.
     */
    private void confirmAction(boolean isBoarding) {
        if (mCurrentDialog != null && mCurrentDialog.isShowing()) {
            mCurrentDialog.dismiss();
            mCurrentDialog = null;
        }

        if (isBoarding) {
            triggerVibrationPattern(BOARD_CONFIRM_VIB);
            Toast.makeText(this, "Boarding confirmed", Toast.LENGTH_LONG).show();
            speak(getString(R.string.tts_boarding_confirmed));
            
            // Auto-reset to welcome screen after boarding to simulate arrival completion
            mMainRoot.postDelayed(new Runnable() {
                @Override
                public void run() {
                    resetToWelcomeScreen();
                }
            }, 6000);
        } else {
            triggerVibrationPattern(SKIP_CONFIRM_VIB);
            Toast.makeText(this, "Bus skipped", Toast.LENGTH_SHORT).show();
            speak(getString(R.string.tts_skipped_confirmed));
            
            // Simulate updating dashboard to a new incoming bus after skipping
            simulateNewBusDetails();
        }
    }

    /**
     * Cancels the current dialog and returns to dashboard.
     */
    private void cancelAction() {
        if (mCurrentDialog != null && mCurrentDialog.isShowing()) {
            mCurrentDialog.dismiss();
            mCurrentDialog = null;
        }
        triggerVibration(SHORT_VIB);
        speak(getString(R.string.tts_action_cancelled));
    }

    /**
     * Simulates fetching new bus details after skipping.
     */
    private void simulateNewBusDetails() {
        TextView busNum = findViewById(R.id.bus_num_text);
        TextView busRoute = findViewById(R.id.bus_route_text);
        TextView busEta = findViewById(R.id.bus_eta_text);
        TextView busStatus = findViewById(R.id.bus_status_text);
        
        TextView stopCurrent = findViewById(R.id.stop_item_current);
        TextView stopNext1 = findViewById(R.id.stop_item_next_1);
        TextView stopNext2 = findViewById(R.id.stop_item_next_2);
        TextView stopNext3 = findViewById(R.id.stop_item_next_3);
        TextView stopDest = findViewById(R.id.stop_item_destination);

        // Update to new values
        busNum.setText("Bus 250");
        busNum.setContentDescription("Incoming Bus Number 250");
        busRoute.setText("Express North to Airport");
        busEta.setText("12 Minutes");
        busStatus.setText("En Route");
        
        stopCurrent.setText("Current Stop: 4th & Pine");
        stopNext1.setText("Next: 15th Ave (4 min)");
        stopNext2.setText("Then: Northgate Transit (8 min)");
        stopNext3.setText("Then: Highway 99 (10 min)");
        stopDest.setText("Terminus: SeaTac Airport (18 min)");

        speak("Dashboard updated. Next incoming Bus 250 to Airport. Arriving in 12 minutes. Double-tap to board this bus. Long-press to skip.");
    }

    /**
     * Resets the application state back to the touch-to-start screen.
     */
    private void resetToWelcomeScreen() {
        mIsDashboardActive = false;
        mWelcomeOverlay.setVisibility(View.VISIBLE);
        
        // Restore default layout values
        TextView busNum = findViewById(R.id.bus_num_text);
        TextView busRoute = findViewById(R.id.bus_route_text);
        TextView busEta = findViewById(R.id.bus_eta_text);
        TextView busStatus = findViewById(R.id.bus_status_text);
        
        TextView stopCurrent = findViewById(R.id.stop_item_current);
        TextView stopNext1 = findViewById(R.id.stop_item_next_1);
        TextView stopNext2 = findViewById(R.id.stop_item_next_2);
        TextView stopNext3 = findViewById(R.id.stop_item_next_3);
        TextView stopDest = findViewById(R.id.stop_item_destination);

        busNum.setText(R.string.bus_number);
        busNum.setContentDescription("Incoming Bus Number 102");
        busRoute.setText(R.string.bus_route_value);
        busEta.setText(R.string.bus_eta_value);
        busStatus.setText(R.string.bus_status_value);
        
        stopCurrent.setText(R.string.stop_current);
        stopNext1.setText(R.string.stop_next_1);
        stopNext2.setText(R.string.stop_next_2);
        stopNext3.setText(R.string.stop_next_3);
        stopDest.setText(R.string.stop_destination);
        
        speak(getString(R.string.tts_welcome));
    }

    @Override
    protected void onDestroy() {
        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
        }
        super.onDestroy();
    }

    /**
     * Custom Gesture Listener for handling taps and holds on the activity view.
     */
    private class MainGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            // Must return true to receive subsequent gesture events
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (!mIsDashboardActive) {
                activateDashboard();
            } else {
                // If dashboard is active, double tap initiates Boarding confirmation
                triggerVibration(SHORT_VIB);
                showConfirmationDialog(true);
            }
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if (mIsDashboardActive) {
                // If dashboard is active, long press initiates Skip confirmation
                triggerVibration(SHORT_VIB);
                showConfirmationDialog(false);
            } else {
                // Speak instructions on long press on welcome overlay
                speakInstructions();
            }
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            // Speak current state details for visibility guidance
            speakInstructions();
            return true;
        }
    }
}
