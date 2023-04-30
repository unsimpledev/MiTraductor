package app.unsimpledev.mitraductor;

import com.android.volley.Response;

public interface Translator {

    public void translateText(String text, String inputLanguage, String outputLanguage, Response.Listener<String> listener, Response.ErrorListener errorListener);

}
