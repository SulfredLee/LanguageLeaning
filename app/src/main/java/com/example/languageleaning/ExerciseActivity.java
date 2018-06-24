package com.example.languageleaning;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class ExerciseActivity extends AppCompatActivity {
    private final String this_class_name = this.getClass().getSimpleName();


    // UI elements
    private TextView textView_category, textView_status;
    private EditText editText_eng, editText_chi;
    private Button button_speak_eng, button_show_eng, button_show_chi, button_next, button_back_home;

    private JSONArray json_words, json_words_exercise, json_words_done;
    private JSONObject json_current_word;
    private int next_word_index = 0;
    private TextToSpeech tts;
    private enum ExerciseStatus {
        TESTING, CHECKING;
    }
    private ExerciseStatus current_status = ExerciseStatus.TESTING;
    private String exercise_type = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(this_class_name, "onCreate(): IN");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        exercise_type = intent.getStringExtra(MainActivity.EXERCISE_TYPE);

        Log.d("myApp", "exercise type: " + exercise_type);

        // init UI component
        textView_category = textView_category == null ? (TextView) findViewById(R.id.textView_category) : textView_category;
        textView_status = textView_status == null ? (TextView) findViewById(R.id.textView_status) : textView_status;
        editText_eng = editText_eng == null ? (EditText) findViewById(R.id.editText_eng) : editText_eng;
        editText_chi = editText_chi == null ? (EditText) findViewById(R.id.editText_chi) : editText_chi;
        button_speak_eng = button_speak_eng == null ? (Button) findViewById(R.id.button_speak) : button_speak_eng;
        button_show_eng = button_show_eng == null ? (Button) findViewById(R.id.button_show_eng) : button_show_eng;
        button_show_chi = button_show_chi == null ? (Button) findViewById(R.id.button_show_chi) : button_show_chi;
        button_next = button_next == null ? (Button) findViewById(R.id.button_next) : button_next;
        button_back_home = button_back_home == null ? (Button) findViewById(R.id.button_back_home) : button_back_home;
        // init json for testing
        // String jsonStr = initJSONForTest().toString();
        // writeToFile(jsonStr);
        // init words array
        try {
            // get words from file
            JSONObject object = new JSONObject(readFromFile());
            json_words = object.getJSONArray("Words");
            next_word_index = 0;
            json_words_done = new JSONArray();
            json_words_exercise = new JSONArray();
            switch (exercise_type) {
                case "error":
                    initJSONWords_error();
                    break;
                case "time":
                    initJSONWords_time();
                    break;
                case "random":
                    initJSONWords_random();
                    break;
                case "category":
                    initJSONWords_category();
                    break;
                case "category_time":
                    initJSONWords_category_time();
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // init text to speech
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    tts.setLanguage(Locale.UK);
                }
            }
        });
        // init first screen
        updateNextWord();
        // init auto scrolling status bar
        textView_status.setSelected(true);
        // init callback
        button_speak_eng.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                speakEng();
            }
        });
        button_show_eng.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String eng_word = "";
                try {
                    eng_word= json_current_word.getString("eng_word");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (eng_word.equals(editText_eng.getText().toString())) {
                    editText_eng.setText("");
                } else {
                    editText_eng.setText(eng_word);
                }
            }
        });
        button_show_chi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String chi_word = "";
                try {
                    chi_word = json_current_word.getString("chi_word");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (chi_word.equals(editText_chi.getText().toString())) {
                    editText_chi.setText("");
                } else {
                    editText_chi.setText(chi_word);
                }
            }
        });
        button_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (current_status) {
                    case TESTING:
                        checkAnswer();
                        current_status = ExerciseStatus.CHECKING;
                        break;
                    case CHECKING:
                        updateNextWord();
                        current_status = ExerciseStatus.TESTING;
                        break;
                }
                updateViewsStatus();
            }
        });
        button_back_home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveWords();
                finish();
            }
        });
        updateViewsStatus();
        Log.i(this_class_name, "onCreate(): OUT");
    }
    private void initJSONWords_category() {
        List<JSONObject> jsonValues = new ArrayList<JSONObject>();
        try {
            for (int i = 0; i < json_words.length(); i++) {
                jsonValues.add(json_words.getJSONObject(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Collections.sort(jsonValues, new Comparator<JSONObject>() { // sorting in decreasing order
            private static final String KEY_NAME = "category";
            @Override
            public int compare(JSONObject a, JSONObject b) {
                String valA, valB;
                try {
                    valA = a.getString(KEY_NAME);
                    valB = b.getString(KEY_NAME);
                    return valA.compareTo(valB);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return 0;
            }
        });
        for (int i = 0; i < jsonValues.size(); i++) {
            json_words_exercise.put(jsonValues.get(i));
        }
    }
    private void initJSONWords_category_time() {
        List<JSONObject> jsonValues = new ArrayList<JSONObject>();
        try {
            for (int i = 0; i < json_words.length(); i++) {
                jsonValues.add(json_words.getJSONObject(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Collections.sort(jsonValues, new Comparator<JSONObject>() { // sorting in decreasing order
            private static final String KEY_NAME = "category";
            @Override
            public int compare(JSONObject a, JSONObject b) {
                String valA, valB;
                try {
                    valA = a.getString(KEY_NAME);
                    valB = b.getString(KEY_NAME);
                    return valA.compareTo(valB);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return 0;
            }
        });
        Date current_time = new Date();
        try {
            for (int i = 0; i < jsonValues.size(); i++) {
                JSONObject current_object = jsonValues.get(i);
                Date object_time = new Date(current_object.getLong("next_time"));
                if (current_time.compareTo(object_time) >= 0){ // we do exercise on those expired
                    json_words_exercise.put(jsonValues.get(i));
                } else { // we don't touch those need to do in the future
                    json_words_done.put(jsonValues.get(i));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private void initJSONWords_error() {
        List<JSONObject> jsonValues = new ArrayList<JSONObject>();
        try {
            for (int i = 0; i < json_words.length(); i++) {
                jsonValues.add(json_words.getJSONObject(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Collections.sort(jsonValues, new Comparator<JSONObject>() { // sorting in decreasing order
            private static final String KEY_NAME = "error_count";
            @Override
            public int compare(JSONObject a, JSONObject b) {
                int valA, valB;
                try {
                    valA = a.getInt(KEY_NAME);
                    valB = b.getInt(KEY_NAME);
                    if (valA < valB) {
                        return 1;
                    } else if (valA == valB) {
                        return 0;
                    } else {
                        return -1;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return 0;
            }
        });
        for (int i = 0; i < jsonValues.size(); i++) {
            json_words_exercise.put(jsonValues.get(i));
        }
    }
    private void initJSONWords_time() {
        List<JSONObject> jsonValues = new ArrayList<JSONObject>();
        try {
            for (int i = 0; i < json_words.length(); i++) {
                jsonValues.add(json_words.getJSONObject(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Collections.sort(jsonValues, new Comparator<JSONObject>() {
            private static final String KEY_NAME = "next_time";
            @Override
            public int compare(JSONObject a, JSONObject b) {
                Date valA, valB;
                try {
                    valA = new Date(a.getLong("next_time"));
                    valB = new Date(b.getLong("next_time"));
                    return valA.compareTo(valB);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return 0;
            }
        });
        Date current_time = new Date();
        try {
            for (int i = 0; i < jsonValues.size(); i++) {
                JSONObject current_object = jsonValues.get(i);
                Date object_time = new Date(current_object.getLong("next_time"));
                if (current_time.compareTo(object_time) >= 0){ // we do exercise on those expired
                    json_words_exercise.put(jsonValues.get(i));
                } else { // we don't touch those need to do in the future
                    json_words_done.put(jsonValues.get(i));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private void initJSONWords_random() {
        List<JSONObject> jsonValues = new ArrayList<JSONObject>();
        try {
            for (int i = 0; i < json_words.length(); i++) {
                jsonValues.add(json_words.getJSONObject(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Collections.shuffle(jsonValues);
        for (int i = 0; i < jsonValues.size(); i++) {
            json_words_exercise.put(jsonValues.get(i));
        }
    }
    private void updateViewsStatus() {
        if (current_status == ExerciseStatus.TESTING) {
            button_next.setText(R.string.button_check);
            button_show_eng.setAlpha(1);
            button_show_chi.setAlpha(1);
            button_show_eng.setClickable(true);
            button_show_chi.setClickable(true);
        }
        if (current_status == ExerciseStatus.CHECKING) {
            button_next.setText(R.string.button_next);
            button_show_eng.setAlpha(.5f);
            button_show_chi.setAlpha(.5f);
            button_show_eng.setClickable(false);
            button_show_chi.setClickable(false);
        }
    }
    private void speakEng() {
        String to_speak = null;
        try {
            to_speak = json_current_word.getString("eng_word");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (tts != null) {
            int ret = tts.speak(to_speak, TextToSpeech.QUEUE_FLUSH, null);
        }
    }
    private void checkAnswer() {
        String eng_word = "";
        String chi_word = "";
        try {
            eng_word = json_current_word.getString("eng_word");
            chi_word = json_current_word.getString("chi_word");

            String user_eng_word = editText_eng.getText().toString();
            String user_chi_word = editText_chi.getText().toString();
            if (eng_word.equals(user_eng_word) && chi_word.equals(user_chi_word)) {
                // correct
                editText_eng.setTextColor(Color.GREEN);
                editText_chi.setTextColor(Color.GREEN);
                // update correct count
                int correct_count = json_current_word.getInt("correct_count");
                correct_count++;
                json_current_word.put("correct_count", correct_count);
                // handle random number
                Random rand = new Random();
                int shifter = rand.nextInt(7) - 3; // min = -3, max = 3, rand.nextInt((max - min) + 1) + min
                // update next time
                Date next_time = new Date(json_current_word.getLong("next_time"));
                Calendar c = Calendar.getInstance();
                c.setTime(next_time);
                if (correct_count == 1) {
                    c.add(Calendar.HOUR, 8 + shifter);
                } else if (correct_count == 2) {
                    c.add(Calendar.HOUR, 24 + shifter);
                } else {
                    c.add(Calendar.DATE, 7 + shifter);
                }
                next_time = c.getTime();
                json_current_word.put("next_time", next_time.getTime());
            } else {
                editText_eng.setText(user_eng_word + "  ----  " + eng_word);
                editText_eng.setTextColor(Color.RED);
                editText_chi.setText(chi_word);
                editText_chi.setTextColor(Color.RED);
                // update error count
                int error_count = json_current_word.getInt("error_count");
                error_count++;
                json_current_word.put("error_count", error_count);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private void updateNextWord() {
        try {
            // save used word
            if (json_current_word != null) {
                json_words_done.put(json_current_word);
                next_word_index++;
            }
            // get next word
            if (json_words_exercise != null && next_word_index < json_words_exercise.length()) {
                json_current_word = json_words_exercise.getJSONObject(next_word_index);
                String category = json_current_word.getString("category");
                String eng_word = json_current_word.getString("eng_word");
                String chi_word = json_current_word.getString("chi_word");
                if (textView_category != null) {
                    textView_category.setText(category);
                }
                if (editText_eng != null) {
                    editText_eng.setText("");
                    editText_eng.setTextColor(Color.BLACK);
                }
                if (editText_chi != null) {
                    editText_chi.setText("");
                    editText_chi.setTextColor(Color.BLACK);
                }
                speakEng();
            } else {
                // NavUtils.navigateUpFromSameTask(this);
                saveWords();
                finish();
                return;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        updateStatus();
    }
    private void saveWords() {
        json_words = null;
        json_words = new JSONArray();
        try {
            if (json_words_done != null) {
                for (int i = 0; i < json_words_done.length(); i++) {
                    json_words.put(json_words_done.getJSONObject(i));
                }
            }
            if (json_words_exercise != null) {
                for (int i = next_word_index; i < json_words_exercise.length(); i++) { // we do this if the user exit before finishing the exercise
                    json_words.put(json_words_exercise.getJSONObject(i));
                }
            }
            if (json_words != null && json_words.length() != 0) {
                JSONObject studentsObj = new JSONObject();
                try{
                    studentsObj.put("Words", json_words);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                writeToFile(studentsObj.toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private void updateStatus() {
        try {
            Date nextTime = new Date(json_current_word.getLong("next_time"));
            int errorCount = json_current_word.getInt("error_count");
            textView_status.setText("current: " + next_word_index + " total: " + json_words_exercise.length() + " expired: " + nextTime.toString() + " error: " + errorCount);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private JSONObject initJSONForTest() {
        JSONObject word1 = new JSONObject();
        Date current_time = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(current_time);
        c.add(Calendar.DATE, 4);
        current_time = c.getTime();
        try {
            word1.put("category", "a,an = 不,否定");
            word1.put("eng_word", "asymmetric");
            word1.put("chi_word", "(a)不對稱的");
            word1.put("error_count", 0);
            word1.put("correct_count", 0);
            word1.put("next_time", current_time.getTime());
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        JSONObject word2 = new JSONObject();
        try {
            word2.put("category", "a,an = 不,否定");
            word2.put("eng_word", "atheist");
            word2.put("chi_word", "(n)無神論");
            word2.put("error_count", 10);
            word2.put("correct_count", 0);
            word2.put("next_time", current_time.getTime());

        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        JSONArray jsonArray = new JSONArray();

        jsonArray.put(word1);
        jsonArray.put(word2);

        JSONObject studentsObj = new JSONObject();
        try{
            studentsObj.put("Words", jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return studentsObj;
    }
    private void writeToFile(String data) {
        try {
            String pathString = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/LanguageLearning.json";
            OutputStream out = new FileOutputStream(pathString);
            out.write(data.getBytes());
            out.flush();
            out.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private String readFromFile() {
        String ret = "";

        try {
            InputStream inputStream = new FileInputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/LanguageLearning.json");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }
}
