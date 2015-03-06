package io.v.v23.vdl;

public enum Kind {
    ANY,
    OPTIONAL,

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
    STRUCT,
    UNION
}
