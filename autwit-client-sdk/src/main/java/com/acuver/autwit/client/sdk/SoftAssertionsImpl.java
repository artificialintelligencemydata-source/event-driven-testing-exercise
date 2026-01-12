package com.acuver.autwit.client.sdk;

import org.testng.asserts.SoftAssert;

class SoftAssertionsImpl implements Autwit.ContextAccessor.SoftAssertions {
    @Override
    public SoftAssert getSoftAssert() {
        try {
            Class<?> utilsClass = Class.forName("com.acuver.autwit.internal.asserts.SoftAssertUtils");
            java.lang.reflect.Method method = utilsClass.getMethod("getSoftAssert");
            return (SoftAssert) method.invoke(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get SoftAssert", e);
        }
    }

    @Override
    public void assertAll() {
        try {
            Class<?> utilsClass = Class.forName("com.acuver.autwit.internal.asserts.SoftAssertUtils");
            java.lang.reflect.Method method = utilsClass.getMethod("assertAll");
            method.invoke(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke assertAll", e);
        }
    }
}
