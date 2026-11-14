package com.moataz.motazalqmi;
public final class NativeIdentity {
    static { System.loadLibrary("motaz_native"); }
    private NativeIdentity() {}
    public static native String abi();
}
