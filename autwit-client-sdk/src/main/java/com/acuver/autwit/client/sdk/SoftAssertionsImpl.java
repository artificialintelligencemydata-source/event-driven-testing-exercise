package com.acuver.autwit.client.sdk;


import org.testng.asserts.SoftAssert;

/**
 * Soft Assertions Implementation.
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
class SoftAssertionsImpl implements Autwit.ContextAccessor.SoftAssertions {

    //private static final Logger log = LogManager.getLogger(SoftAssertionsImpl.class);

    @Override
    public SoftAssert getSoftAssert() {
        try {
            // Try new package first
            Class<?> utilsClass = Class.forName("com.acuver.autwit.internal.asserts.SoftAssertUtils");
            java.lang.reflect.Method method = utilsClass.getMethod("getSoftAssert");
            return (SoftAssert) method.invoke(null);
        } catch (ClassNotFoundException e1) {
            try {
                // Fall back to old package
                Class<?> utilsClass = Class.forName("com.acuver.automation.api.asserts.SoftAssertUtils");
                java.lang.reflect.Method method = utilsClass.getMethod("getSoftAssert");
                return (SoftAssert) method.invoke(null);
            } catch (Exception e2) {
               // log.warn("SoftAssertUtils not found, creating new SoftAssert");
                return new SoftAssert();
            }
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
        } catch (ClassNotFoundException e1) {
            try {
                Class<?> utilsClass = Class.forName("com.acuver.automation.api.asserts.SoftAssertUtils");
                java.lang.reflect.Method method = utilsClass.getMethod("assertAll");
                method.invoke(null);
            } catch (Exception e2) {
                //log.warn("SoftAssertUtils.assertAll() not available");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke assertAll", e);
        }
    }
}