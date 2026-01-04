package com.acuver.autwit.internal.asserts;
import org.testng.asserts.SoftAssert;

public final class SoftAssertUtils {
    private static final ThreadLocal<SoftAssert> TL = new ThreadLocal<>();

    private SoftAssertUtils(){}

    public static SoftAssert getSoftAssert(){
        if (TL.get() == null) TL.set(new SoftAssert());
        return TL.get();
    }

    public static void assertAll(){
        SoftAssert s = TL.get();
        if (s != null) {
            s.assertAll();
            TL.remove();
        }
    }

    public static void clearSoftAssert(){
        TL.remove();
    }
}
