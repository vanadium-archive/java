package io.veyron.veyron.veyron2.vdl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

import android.util.Base64;

import org.joda.time.Duration;

import io.veyron.veyron.veyron2.VeyronException;

/**
 * JSONUtil provides various utilities for JSON encoding/decoding of VDL types.
 */
public class JSONUtil {
	private static GsonBuilder builder = new GsonBuilder();

	static {
		builder.registerTypeAdapter(byte[].class, new ByteArrayTypeAdapter());
		builder.registerTypeAdapter(Any.class, new AnyTypeAdapter());
		builder.registerTypeAdapter(Duration.class, new DurationTypeAdapter());
		builder.registerTypeAdapter(VeyronException.class, new VeyronExceptionTypeAdapter());
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
				final TypeAdapterFactory factory =
					(TypeAdapterFactory) c.getConstructor().newInstance();
				return factory.create(gson, type);
			} catch (Exception e) {
				return null;
			}
		}
	}

    private static class ByteArrayTypeAdapter implements JsonSerializer<byte[]>,
			JsonDeserializer<byte[]>{
		@Override
		public JsonElement serialize(
				byte[] src, java.lang.reflect.Type type, JsonSerializationContext ctx) {
			return new JsonPrimitive(Base64.encodeToString(src, Base64.NO_WRAP));
      	}
		@Override
		public byte[] deserialize(
				JsonElement json, java.lang.reflect.Type type, JsonDeserializationContext ctx)
				throws JsonParseException {
			final String encoded = json.getAsJsonPrimitive().getAsString();
			try {
				return Base64.decode(encoded, Base64.NO_WRAP);
			} catch (IllegalArgumentException e) {
				throw new JsonParseException(e.getMessage());
			}
		}
	}

	// TODO(spetrovic): figure out how to decode fields of type Any.
	private static class AnyTypeAdapter implements JsonSerializer<Any>, JsonDeserializer<Any>{
		@Override
		public JsonElement serialize(
		    Any src, java.lang.reflect.Type type, JsonSerializationContext ctx) {
			return new JsonNull();
		}
		@Override
		public Any deserialize(
				JsonElement json, java.lang.reflect.Type type, JsonDeserializationContext ctx)
				throws JsonParseException {
			return null;
		}
	}

	// TODO(spetrovic): move this one out into package "io.veyron.veyron.veyron2" as it's not VDL-specific.
	private static class DurationTypeAdapter implements JsonSerializer<Duration>,
			JsonDeserializer<Duration>{
		@Override
		public JsonElement serialize(
				Duration src, java.lang.reflect.Type type, JsonSerializationContext ctx) {
			return new JsonPrimitive(src.getMillis() * 1000000L);
      	}
		@Override
		public Duration deserialize(
				JsonElement json, java.lang.reflect.Type type, JsonDeserializationContext ctx)
				throws JsonParseException {
			final long durationNanos = json.getAsJsonPrimitive().getAsLong();
			return Duration.millis(durationNanos / 1000000L);
		}
	}
	// TODO(spetrovic): move this one out into package "io.veyron.veyron.veyron2" as it's not VDL-specific.
	private static class VeyronExceptionTypeAdapter implements JsonSerializer<VeyronException>,
			JsonDeserializer<VeyronException>{
		@Override
		public JsonElement serialize(
				VeyronException src, java.lang.reflect.Type type, JsonSerializationContext ctx) {
			return new JsonPrimitive(src.getMessage());
      	}
		@Override
		public VeyronException deserialize(
				JsonElement json, java.lang.reflect.Type type, JsonDeserializationContext ctx)
				throws JsonParseException {
			return new VeyronException(json.getAsJsonPrimitive().getAsString());
		}
	}
}