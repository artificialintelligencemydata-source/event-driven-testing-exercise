package com.acuver.autwit.internal.exceptions;
/**
 * Internal framework exception for skipping already-passed steps.
 *
 * This exception is used ONLY by the @BeforeStep hook in the internal testkit
 * to signal that a step has already been executed successfully in a previous
 * scenario execution and should be bypassed during resume.
 *
 * CRITICAL DISTINCTIONS:
 * - NOT visible to client code
 * - NOT the same as org.testng.SkipException (which pauses scenarios)
 * - Used ONLY by framework internals
 * - Caught by Cucumber, marks step SKIPPED in report
 * - Does NOT trigger scenario-level pause logic
 */
public class InternalStepSkipException extends RuntimeException {

    public InternalStepSkipException(String message) {
        super(message);
    }

    public InternalStepSkipException(String message, Throwable cause) {
        super(message, cause);
    }
}
