package com.example.languageleaning;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private final String this_class_name = this.getClass().getSimpleName();
    private Context mContext = MainActivity.this;
    private static final int WRITE_STORAGE_REQUEST = 112;
    public static final String EXERCISE_TYPE = "time";

    // UI elements
    private RadioGroup radioGroup_selections, radioGroup_selections_2;
    private RadioButton radioButton_error, radioButton_time, radioButton_random, radioButton_category, radioButton_category_time;
    private TextView textView_info;
    private Button button_start;

    /**
     * Get the method name for a depth in call stack. <br />
     * Utility function
     * @param depth depth in the call stack (0 means current method, 1 means call method, ...)
     * @return method name
     */
    public static String getMethodName(final int depth)
    {
        final StackTraceElement[] ste = Thread.currentThread().getStackTrace();

        //System. out.println(ste[ste.length-depth].getClassName()+"#"+ste[ste.length-depth].getMethodName());
        // return ste[ste.length - depth].getMethodName();  //Wrong, fails for depth = 0
        return ste[ste.length - 1 - depth].getMethodName(); //Thank you Tom Tresansky
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(this_class_name, "onCreate(): IN");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI component
        radioGroup_selections = radioGroup_selections == null ? (RadioGroup) findViewById(R.id.radioGroup_selections) : radioGroup_selections;
        radioGroup_selections_2 = radioGroup_selections_2 == null ? (RadioGroup) findViewById(R.id.radioGroup_selections_2) : radioGroup_selections_2;
        radioButton_error = radioButton_error == null ? (RadioButton) findViewById(R.id.radioButton_error) : radioButton_error;
        radioButton_time = radioButton_time == null ? (RadioButton) findViewById(R.id.radioButton_time) : radioButton_time;
        radioButton_random = radioButton_random == null ? (RadioButton) findViewById(R.id.radioButton_random) : radioButton_random;
        radioButton_category = radioButton_category == null ? (RadioButton) findViewById(R.id.radioButton_category) : radioButton_category;
        radioButton_category_time = radioButton_category_time == null ? (RadioButton) findViewById((R.id.radioButton_category_time)) : radioButton_category_time;
        textView_info = textView_info == null ? (TextView) findViewById(R.id.textView_intro) : textView_info;
        button_start = button_start == null ? (Button) findViewById(R.id.button_start) : button_start;

        // handle permission run time
        if (Build.VERSION.SDK_INT >= 23) {
            String[] PERMISSIONS = {android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
            if (!hasPermissions(mContext, PERMISSIONS)) {
                ActivityCompat.requestPermissions((Activity) mContext, PERMISSIONS, WRITE_STORAGE_REQUEST );
            } else {
                // already have permission
            }
        } else {
            // no need to get permission
        }
        // handle callback
        radioButton_error.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                radioGroup_selections_2.clearCheck();
                textView_info.setText(R.string.error_info);
            }
        });
        radioButton_time.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                radioGroup_selections_2.clearCheck();
                textView_info.setText(R.string.time_info);
            }
        });
        radioButton_random.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                radioGroup_selections_2.clearCheck();
                textView_info.setText(R.string.random_info);
            }
        });
        radioButton_category.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                radioGroup_selections.clearCheck();
                textView_info.setText("Exercise will be sorted by category");
            }
        });
        radioButton_category_time.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                radioGroup_selections.clearCheck();
                textView_info.setText("Exercise will be sorted by category and time");
            }
        });
        button_start.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startExercise(v);
            }
        });
        Log.i(this_class_name, "onCreate(): OUT");
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case WRITE_STORAGE_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //do here
                    new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download").mkdirs();
                } else {
                    Toast.makeText(mContext, "The app was not allowed to write in your storage", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
    private void startExercise(View view) {
        Log.i(this_class_name, "startExercise(): IN");
        Intent intent = new Intent(this, ExerciseActivity.class);
        int selected = radioGroup_selections.getCheckedRadioButtonId();
        if (selected == -1) {
            selected = radioGroup_selections_2.getCheckedRadioButtonId();
        }
        switch (selected) {
            case R.id.radioButton_error:
                intent.putExtra(EXERCISE_TYPE, "error");
                break;
            case R.id.radioButton_time:
                intent.putExtra(EXERCISE_TYPE, "time");
                break;
            case R.id.radioButton_random:
                intent.putExtra(EXERCISE_TYPE, "random");
                break;
            case R.id.radioButton_category:
                intent.putExtra(EXERCISE_TYPE, "category");
                break;
            case R.id.radioButton_category_time:
                intent.putExtra(EXERCISE_TYPE, "category_time");
                break;
            default:
                break;
        }
        startActivity(intent);
        Log.i(this_class_name, "startExercise(): OUT");
    }
    private static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
}
