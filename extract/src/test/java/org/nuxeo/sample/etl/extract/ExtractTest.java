package org.nuxeo.sample.etl.extract;

import org.junit.Test;

public class ExtractTest {

    @Test
    public void testAppWithChronicle() {
        new App().run(false);
    }

    @Test
    public void testAppWithKafka() {
        new App().run(true);
    }
}
