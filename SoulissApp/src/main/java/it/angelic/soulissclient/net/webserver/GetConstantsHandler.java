package it.angelic.soulissclient.net.webserver;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import it.angelic.soulissclient.model.typicals.Constants;

public class GetConstantsHandler implements HttpRequestHandler {
	private Context context = null;
	final String contentType = "text/html; charset=UTF-8";
	private SharedPreferences pref;

	public GetConstantsHandler(Context context) {
		this.context = context;
		pref = PreferenceManager.getDefaultSharedPreferences(context);

	}

	

	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext httpContext) throws HttpException,
			IOException {
		//LOGIN STUFF
		try {
			Zozzariello.doLogin(request, response, httpContext, pref);
		} catch (Exception e) {
			if ("".compareTo(pref.getString("webUser", "")) == 0) {
				// user disabilitata nelle opzioni
			} else {
				//non si passa
				response.addHeader("WWW-Authenticate", "Basic");
				response.setStatusCode(HttpStatus.SC_UNAUTHORIZED);
				return;
			}
		}

		HttpEntity entity = new EntityTemplate(new ContentProducer() {
			public void writeTo(final OutputStream outstream) throws IOException {
				OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8");
				Constants pieruzzo = new Constants();
				String resp = pieruzzo.toString();

				writer.write(resp);
				writer.flush();
			}
		});

		((EntityTemplate) entity).setContentType(contentType);

		response.setEntity(entity);

	}

}
