package app.unsimpledev.mitraductor;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener{

    private EditText editTextInputText;
    private EditText editTextOutputText;
    private ProgressBar progressBar;
    private Spinner spinnerInputLanguage;
    private Spinner spinnerOutputLanguage;

    private List<Language> languages;
    private Language inputLanguage;
    private Language outputLanguage;

    private Translator translator;

    private TextToSpeech textToSpeech;

    private boolean isTextToSpeechInitialized = false;


    private static final String SP_FILE_NAME = "SP_FILE_NAME_APP";
    private static final String SP_INPUT_LANGUAGE_CODE = "SP_INPUT_LANGUAGE_CODE";
    private static final String SP_OUTPUT_LANGUAGE_CODE = "SP_OUTPUT_LANGUAGE_CODE";
    private static final String SP_CHECK_VOICE_ENABLED = "SP_CHECK_VOICE_ENABLED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        translator = new TranslatorGPT35(this);

        editTextInputText = findViewById(R.id.editTextInputText);
        editTextOutputText = findViewById(R.id.editTextOutputText);
        progressBar = findViewById(R.id.progressBar);

        textToSpeech = new TextToSpeech(this, this);

        this.loadLanguages();

        String inputLangCodeSaved = getSharedPreferences(SP_FILE_NAME, MODE_PRIVATE).getString(SP_INPUT_LANGUAGE_CODE, "es");
        String outputLangCodeSaved = getSharedPreferences(SP_FILE_NAME, MODE_PRIVATE).getString(SP_OUTPUT_LANGUAGE_CODE, "en");


        inputLanguage = findLanguage(inputLangCodeSaved);
        outputLanguage = findLanguage(outputLangCodeSaved);

        spinnerInputLanguage = findViewById(R.id.spinnerInputLanguage);
        spinnerOutputLanguage = findViewById(R.id.spinnerOutputLanguage);

        setupSpinnerAdapter(spinnerInputLanguage, inputLanguage);
        setupSpinnerAdapter(spinnerOutputLanguage, outputLanguage);

        spinnerInputLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                inputLanguage = (Language) parent.getItemAtPosition(position);
                getSharedPreferences(SP_FILE_NAME, MODE_PRIVATE).edit().putString(SP_INPUT_LANGUAGE_CODE, inputLanguage.getCode()).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //No hago nada
            }
        });

        spinnerOutputLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                outputLanguage = (Language) parent.getItemAtPosition(position);
                getSharedPreferences(SP_FILE_NAME, MODE_PRIVATE).edit().putString(SP_OUTPUT_LANGUAGE_CODE, outputLanguage.getCode()).apply();
                Locale locale = new Locale(outputLanguage.getCode());
                changeVoiceLanguage(TextToSpeech.SUCCESS, locale);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //No hago nada
            }
        });

        Button translate = findViewById(R.id.buttonTranslate);
        translate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = editTextInputText.getText().toString();
                callTranslator(text);
            }
        });

        Button speech = findViewById(R.id.buttonInputVoice);
        speech.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Puedes hablar");
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, inputLanguage.getCode());
                startActivitySpeechRecognizerIntent.launch(intent);
            }
        });

        CheckBox checkVoiceResult = findViewById(R.id.checkOutputVoice);
        checkVoiceResult.setChecked(getSharedPreferences(SP_FILE_NAME, MODE_PRIVATE).getBoolean(SP_CHECK_VOICE_ENABLED, true));
        checkVoiceResult.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                getSharedPreferences(SP_FILE_NAME, MODE_PRIVATE).edit().putBoolean(SP_CHECK_VOICE_ENABLED, isChecked).apply();
            }
        });
    }

    ActivityResultLauncher<Intent> startActivitySpeechRecognizerIntent = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK){
                        List<String> results = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        String text = results.get(0);
                        editTextInputText.setText(text);
                        callTranslator(text);
                    }
                }
            });

    private void loadLanguages(){
        languages = new ArrayList<>();
        languages.add(new Language("es", "Español") );
        languages.add(new Language("en", "Inglés") );
        languages.add(new Language("pt", "Portugués") );
    }


    private Language findLanguage(String langCode){
        for (Language l : languages){
            if (l.getCode().equals(langCode)){
                return l;
            }
        }
        return null;
    }

    private void setupSpinnerAdapter(Spinner spinner, Language initValue){
        ArrayAdapter<Language> adapter = new ArrayAdapter<Language>(this, android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        //Inicializo con valor inicial
        int initialPosition = adapter.getPosition(initValue);
        if (initialPosition >= 0){
            spinner.setSelection(initialPosition);//Establece el valor en el spinner
        }
    }

    private void callTranslator(String text){
        progressBar.setVisibility(View.VISIBLE);
        translator.translateText(text, inputLanguage.getDescription(), outputLanguage.getDescription(), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                progressBar.setVisibility(View.GONE);
                response = response.replaceFirst("^(\n)+","");
                response = response.replaceFirst("^\\{","");
                response = response.replaceFirst("\\}$","");
                editTextOutputText.setText(response);
                callTextToSpeech(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onInit(int status) {
        isTextToSpeechInitialized = true;
        String outputLanguageCode = getSharedPreferences(SP_FILE_NAME, MODE_PRIVATE).getString(SP_INPUT_LANGUAGE_CODE, "en");
        Locale locale = new Locale(outputLanguageCode);
        changeVoiceLanguage(status, locale);
    }

    private void changeVoiceLanguage(int status, Locale locale){
        if (isTextToSpeechInitialized) {
            if (status == TextToSpeech.SUCCESS) {
                if (textToSpeech.isLanguageAvailable(locale) == TextToSpeech.LANG_AVAILABLE) {
                    int result = textToSpeech.setLanguage(locale);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TextToSpeech", "Lenguaje no soportado");
                    }
                } else {
                    Toast.makeText(this, "Idioma no soportado", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e("TextToSpeech", "Inicializacion fallida");
            }
        }
    }

    private void callTextToSpeech(String text){
        if (getSharedPreferences(SP_FILE_NAME, MODE_PRIVATE).getBoolean(SP_CHECK_VOICE_ENABLED, true)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }
}