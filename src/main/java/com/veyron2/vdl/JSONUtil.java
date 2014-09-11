package com.veyron2.vdl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

import android.util.Base64;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * JSONUtil provides various utilities for JSON encoding/decoding of VDL types.
 */
public class JSONUtil {
	private static GsonBuilder builder = new GsonBuilder(); 

	static {
		builder.registerTypeAdapter(byte[].class, new ByteArrayTypeAdapter());
		builder.registerTypeAdapterFactory(new CustomTypeAdapterFactory());
	}

	/**
	 * Returns an instance of GsonBuilder that should be used to serialize/deserialize VDL types.
	 *
	 * @return instance of GsonBuilder.
	 */
	public static GsonBuilder getGsonBuilder() {
		return builder;
	}

	private static class CustomTypeAdapterFactory implements TypeAdapterFactory {
		@Override
		public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
			final TypeToken<TypeAdapterFactory> factoryType = new TypeToken<TypeAdapterFactory>(){};
			if (!factoryType.isAssignableFrom(type)) {
				return null;
			}
			try {
				final Class<T> c = (Class<T>) type.getRawType();
				final TypeAdapterFactory factory = (TypeAdapterFactory) c.getConstructor().newInstance();
				return factory.create(gson, type);
			} catch (Exception e) {
				return null;
			}
		}
	}

    private static class ByteArrayTypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]>{
		@Override
		public JsonElement serialize(
			byte[] src, java.lang.reflect.Type type, JsonSerializationContext context) {
			return new JsonPrimitive(Base64.encodeToString(src, Base64.NO_WRAP));
      	}
		@Override
		public byte[] deserialize(
			JsonElement json, java.lang.reflect.Type type, JsonDeserializationContext context)
			throws JsonParseException {
			final String encoded = json.getAsJsonPrimitive().getAsString();
			try {
				return Base64.decode(encoded, Base64.NO_WRAP);
			} catch (IllegalArgumentException e) {
				throw new JsonParseException(e.getMessage());
			}
		}
	}
}