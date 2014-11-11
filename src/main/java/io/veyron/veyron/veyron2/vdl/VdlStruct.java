package io.veyron.veyron.veyron2.vdl;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

/**
 * VdlStruct is a map based representation of a VDL struct.
 */
public class VdlStruct extends AbstractVdlStruct implements Parcelable {
    private final Map<String, VdlValue> fields;
    private final Map<String, VdlType> fieldTypes;

    public VdlStruct(VdlType type) {
        super(type);
        fields = new HashMap<String, VdlValue>();
        fieldTypes = new HashMap<String, VdlType>();
        for (VdlStructField structField : type.getFields()) {
            fieldTypes.put(structField.getName(), structField.getType());
        }
    }

    /**
     * Tries to assign a new value for specified field. Assigns value and returns true if the struct
     * has a field with specified name and the new matches the field type, otherwise returns false.
     *
     * @param name name of the field
     * @param value value to assign
     * @return true iff value is successfully assigned
     */
    public boolean assignField(String name, VdlValue value) {
        VdlType expectedType = fieldTypes.get(name);
        if (expectedType == null || !expectedType.equals(value.getType())) {
            return false;
        }
        fields.put(name, value);
        return true;
    }

    /**
     * Returns value of field with specified name
     *
     * @param name name of the field
     * @return value of field or null if struct has no such field or value was never assigned
     */
    public VdlValue getField(String name) {
        return fields.get(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        final VdlStruct other = (VdlStruct) obj;
        return fields.equals(other.fields);
    }

    @Override
    public int hashCode() {
        return fields.hashCode();
    }

    @Override
    public String toString() {
        return fields.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        // TODO(rogulenko): replace this with vom encoding
        out.writeSerializable(this);
    }

    public static final Creator<VdlStruct> CREATOR = new Creator<VdlStruct>() {
        @Override
        public VdlStruct createFromParcel(Parcel in) {
            // TODO(rogulenko): replace this with vom decoding
            return (VdlStruct) in.readSerializable();
        }

        @Override
        public VdlStruct[] newArray(int size) {
            return new VdlStruct[size];
        }
    };
}
