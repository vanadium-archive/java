package com.veyron.projects.namespace;

import android.graphics.Typeface;
import android.os.Parcel;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.veyron.veyron.veyron2.naming.VDLMountEntry;


/**
 * ViewUtil contains utilities for managing View types used in the Veyron namespace browser.
 */
public class ViewUtil {
	private static final String TAG = "com.veyron.projects.namespace";

	public static LinearLayout createDirectoryView(String text, VDLMountEntry entry,
			LayoutInflater inflater) {
		final LinearLayout dirView = (LinearLayout) inflater.inflate(R.layout.directory_item, null);
		dirView.setTag(entry);
		((TextView) dirView.findViewById(R.id.name)).setText(text);
		updateDirectoryView(dirView, false);
		return dirView;
	}

	public static LinearLayout createObjectView(String text, VDLMountEntry entry,
			LayoutInflater inflater) {
		final LinearLayout objView = (LinearLayout) inflater.inflate(R.layout.object_item, null);
		objView.setTag(entry);
		((TextView) objView.findViewById(R.id.name)).setText(text);
		updateObjectView(objView, false);
		return objView;
	}

	public static LinearLayout createMethodView(String text, VDLMountEntry entry,
			LayoutInflater inflater) {
		final LinearLayout methodView = (LinearLayout) inflater.inflate(R.layout.method_item, null);
		methodView.setTag(entry);
		((TextView) methodView.findViewById(R.id.name)).setText(text);
		return methodView;
	}

	public static void updateDirectoryView(LinearLayout dirView, boolean isActivated) {
		dirView.setActivated(isActivated);
		((TextView) dirView.findViewById(R.id.name)).setTypeface(
				dirView.isActivated() ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
		((ImageView) dirView.findViewById(R.id.arrow)).setRotation(dirView.isActivated() ? 0 : 180);
	}

	public static void updateObjectView(LinearLayout objView, boolean isActivated) {
		objView.setActivated(isActivated);
		((TextView) objView.findViewById(R.id.name)).setTypeface(
				objView.isActivated() ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
		((ImageView) objView.findViewById(R.id.sign)).setImageResource(
				objView.isActivated() ? R.drawable.minus_sign : R.drawable.plus_sign);
	}

	public static void serializeView(View view, Parcel parcel) {
		final int id = view.getId();
		parcel.writeInt(id);
		switch (id) {
			case R.id.directory:
				serializeDirectoryView((LinearLayout) view, parcel);
				return;
			case R.id.object:
				serializeObjectView((LinearLayout) view, parcel);
				return;
			case R.id.method:
				serializeMethodView((LinearLayout) view, parcel);
				return;
			default:
				throw new RuntimeException("Serializing unknown view type with id: " + id);
		}
	}

	public static View deserializeView(Parcel parcel, LayoutInflater inflater) {
		final int id = parcel.readInt();
		switch (id) {
			case R.id.directory:
				return deserializeDirectoryView(parcel, inflater);
			case R.id.object:
				return deserializeObjectView(parcel, inflater);
			case R.id.method:
				return deserializeMethodView(parcel, inflater);
			default:
				throw new RuntimeException("Deserializing unknown view type with id: " + id);
		}
	}

	private static void serializeDirectoryView(LinearLayout dirView, Parcel parcel) {
		//Save own view state.
		parcel.writeString(((TextView) dirView.findViewById(R.id.name)).getText().toString());
		parcel.writeByte((byte) (dirView.isActivated() ? 1 : 0));
		parcel.writeInt(dirView.getChildCount());
		final VDLMountEntry entry = (VDLMountEntry) dirView.getTag();
		parcel.writeParcelable(entry, 0);

		// Save child view state.
		for (int i = 1; i < dirView.getChildCount(); ++i) {
			serializeView(dirView.getChildAt(i), parcel);
		}
	}

	private static void serializeObjectView(LinearLayout objView, Parcel parcel) {
		// Save own view state.
		parcel.writeString(((TextView) objView.findViewById(R.id.name)).getText().toString());
		parcel.writeByte((byte) (objView.isActivated() ? 1 : 0));
		parcel.writeInt(objView.getChildCount());
		final VDLMountEntry entry = (VDLMountEntry) objView.getTag();
		parcel.writeParcelable(entry, 0);

		// Save child view state.
		for (int i = 1; i < objView.getChildCount(); ++i) {
			serializeView(objView.getChildAt(i), parcel);
		}
	}

	private static void serializeMethodView(LinearLayout methodView, Parcel parcel) {
		// Save own view state.
		parcel.writeString(((TextView) methodView.findViewById(R.id.name)).getText().toString());
		parcel.writeByte((byte) (methodView.isActivated() ? 1 : 0));
		parcel.writeInt(methodView.getChildCount());
		final VDLMountEntry entry = (VDLMountEntry) methodView.getTag();
		parcel.writeParcelable(entry, 0);

		// Save child view state.
		for (int i = 1; i < methodView.getChildCount(); ++i) {
			serializeView(methodView.getChildAt(i), parcel);
		}
	}

	private static LinearLayout deserializeDirectoryView(Parcel parcel, LayoutInflater inflater) {
		// Restore own view state.
		final String text = parcel.readString();
		final boolean isActivated = (parcel.readByte() == 1);
		final int numChildren = parcel.readInt();
		final VDLMountEntry entry =
		        (VDLMountEntry) parcel.readParcelable(VDLMountEntry.class.getClassLoader());
		final LinearLayout dirView = createDirectoryView(text, entry, inflater);
		updateDirectoryView(dirView, isActivated);

		// Restore child view state.
		for (int i = 1; i < numChildren; i++) {
			dirView.addView(deserializeView(parcel, inflater));
		}
		return dirView;
	}

	private static LinearLayout deserializeObjectView(Parcel parcel, LayoutInflater inflater) {
		// Restore own view state.
		final String text = parcel.readString();
		final boolean isActivated = (parcel.readByte() == 1);
		final int numChildren = parcel.readInt();
		final VDLMountEntry entry =
		        (VDLMountEntry) parcel.readParcelable(VDLMountEntry.class.getClassLoader());
		final LinearLayout objView = createObjectView(text, entry, inflater);
		updateObjectView(objView, isActivated);

		// Restore child view state.
		for (int i = 1; i < numChildren; i++) {
			objView.addView(deserializeView(parcel, inflater));
		}
		return objView;
	}

	private static LinearLayout deserializeMethodView(Parcel parcel, LayoutInflater inflater) {
		// Restore own view state.
		final String text = parcel.readString();
		@SuppressWarnings("unused")
		final boolean isActivated = (parcel.readByte() == 1);
		final int numChildren = parcel.readInt();
		final VDLMountEntry entry =
		        (VDLMountEntry) parcel.readParcelable(VDLMountEntry.class.getClassLoader());
		final LinearLayout methodView = createMethodView(text, entry, inflater);

		// Restore child view state.
		for (int i = 1; i < numChildren; i++) {
			methodView.addView(deserializeView(parcel, inflater));
		}
		return methodView;
	}
}
