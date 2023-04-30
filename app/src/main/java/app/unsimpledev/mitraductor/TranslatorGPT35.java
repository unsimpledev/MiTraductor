package app.unsimpledev.mitraductor;

import android.content.Context;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class TranslatorGPT35 implements  Translator{

    private final static String BASE_URL = "https://api.openai.com/v1/chat/completions";

    private RequestQueue requestQueue;

    public TranslatorGPT35(Context context){
        requestQueue = Volley.newRequestQueue(context);
    }

    public void translateText(String text, String inputLanguage, String outputLanguage, Response.Listener<String> listener, Response.ErrorListener errorListener){
//        {
//            "model": "gpt-3.5-turbo",
//            "messages": [{"role": "user", "content": "Hello!"}]
//        }
        try{
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", "gpt-3.5-turbo");
            jsonBody.put("temperature", 1);

            JSONArray jsonArrayMessages = new JSONArray();
            JSONObject jsonObjectMessages = new JSONObject();

            jsonObjectMessages.put("role", "user");
            jsonObjectMessages.put("content", creeateStringContent(text, inputLanguage, outputLanguage));

            jsonArrayMessages.put(jsonObjectMessages);
            jsonBody.put("messages", jsonArrayMessages);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, BASE_URL, jsonBody, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    //TODO procesar respuesta
                   /* "choices": [{
                        "index": 0,
                        "message": {
                            "role": "assistant",
                                    "content": "\n\nHello there, how may I assist you today?",
                        },
                        "finish_reason": "stop"
                    }],*/
                    try{
                        JSONObject message = (JSONObject) response.getJSONArray("choices").getJSONObject(0).get("message");
                        String content = message.getString("content");
                        listener.onResponse(content);
                    }catch (Exception e){
                        Log.e("TranslatorGPT35", "Error parseando respuesta ", e);
                        errorListener.onErrorResponse(new VolleyError("Error parseando request"));
                    }
                }
            },errorListener){
                @Override
                public Map<String, String> getHeaders(){
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization" , "Bearer " + Config.CHAT_GPT_API_KEY);
                    return  headers;
                }
            };

            request.setRetryPolicy(new DefaultRetryPolicy(60000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            requestQueue.add(request);
        }catch (Exception e){
            Log.e("TranslatorGPT35", "Error creando request body ", e);
            errorListener.onErrorResponse(new VolleyError("Error creando request"));
        }
    }

    private String creeateStringContent(String texto, String inputLanguage, String outputLanguage){
        StringBuffer content = new StringBuffer();
        content.append("Traduce de ");
        content.append(inputLanguage);
        content.append(" a ");
        content.append(outputLanguage);
        content.append(" el siguiente texto ");
        content.append(" : \"{");
        content.append(texto);
        content.append(" }\"");
        content.append(" .Solo respode el texto traducido entre llaves {}, no agregue comillas");
        return  content.toString();
    }
}
