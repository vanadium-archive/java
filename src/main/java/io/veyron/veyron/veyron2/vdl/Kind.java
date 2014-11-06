// TODO(bprosnitz) Either finish this or remove it before the 0.1 release.

package io.veyron.veyron.veyron2.vdl;

public enum Kind {
    ANY,
    ONE_OF,

    BOOL,
    BYTE,
    UINT16,
    UINT32,
    UINT64,
    INT16,
    INT32,
    INT64,
    FLOAT32,
    FLOAT64,
    COMPLEX64,
    COMPLEX128,
    STRING,
    ENUM,
    TYPEOBJECT,

    ARRAY,
    LIST,
    SET,
    MAP,
    STRUCT
}
