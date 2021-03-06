/********************************************************************************
 * Copyright (c) 2019 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.keyple.core.seproxy.plugin.local;

import static org.eclipse.keyple.core.seproxy.plugin.local.AbstractObservableState.MonitoringState.WAIT_FOR_SE_INSERTION;
import static org.eclipse.keyple.core.seproxy.plugin.local.AbstractObservableState.MonitoringState.WAIT_FOR_START_DETECTION;
import static org.mockito.Mockito.doReturn;
import java.util.concurrent.CountDownLatch;
import org.eclipse.keyple.core.CoreBaseTest;
import org.eclipse.keyple.core.seproxy.event.ObservableReader;
import org.eclipse.keyple.core.seproxy.event.ReaderEvent;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderException;
import org.eclipse.keyple.core.seproxy.plugin.mock.BlankSmartPresenceTheadedReader;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * test the feature of SmartRemoval methods
 */
@RunWith(Parameterized.class)
public class AbsSmartPresenceTheadedReaderTest extends CoreBaseTest {

    private static final Logger logger =
            LoggerFactory.getLogger(AbsSmartPresenceTheadedReaderTest.class);


    final String PLUGIN_NAME = "AbsSmartPresenceTheadedReaderTestP";
    final String READER_NAME = "AbsSmartPresenceTheadedReaderTest";

    BlankSmartPresenceTheadedReader r;

    // Execute tests 10 times
    @Parameterized.Parameters
    public static Object[][] data() {
        int x = 0;
        return new Object[x][0];
    }


    @Before
    public void setUp() throws KeypleReaderException {
        logger.info("------------------------------");
        logger.info("Test {}", name.getMethodName() + "");
        logger.info("------------------------------");

        r = getSmartSpy(PLUGIN_NAME, READER_NAME);
    }

    /*
     */
    @After
    public void tearDown() throws Throwable {
        r.clearObservers();
        r = null;

    }

    @Test
    public void startRemovalSequence() throws Exception {

        // SE matched
        doReturn(true).when(r).processSeInserted();

        r.addObserver(getObs());
        Thread.sleep(100);

        r.startRemovalSequence();
        Thread.sleep(100);

        // does nothing
        Assert.assertEquals(WAIT_FOR_START_DETECTION, r.getCurrentMonitoringState());
    }

    @Test
    public void startRemovalSequence_CONTINUE() throws Exception {

        // SE matched
        doReturn(true).when(r).processSeInserted();
        // use mocked BlankSmartPresenceTheadedReader methods

        r.addObserver(getObs());
        r.startSeDetection(ObservableReader.PollingMode.REPEATING);// WAIT_FOR_SE_INSERTION
        Thread.sleep(100);

        r.startRemovalSequence();
        Thread.sleep(100);

        Assert.assertEquals(WAIT_FOR_SE_INSERTION, r.getCurrentMonitoringState());
    }

    @Test
    public void startRemovalSequence_noping_STOP() throws Exception {

        // SE matched
        doReturn(true).when(r).processSeInserted();
        doReturn(false).when(r).isSePresentPing();

        r.addObserver(getObs());
        r.startSeDetection(ObservableReader.PollingMode.SINGLESHOT);
        Thread.sleep(100);

        r.startRemovalSequence();
        Thread.sleep(100);

        Assert.assertEquals(WAIT_FOR_SE_INSERTION, r.getCurrentMonitoringState());
    }


    @Test
    public void startRemovalSequence_ping_STOP() throws Exception {

        // SE matched
        doReturn(true).when(r).processSeInserted();
        // doReturn(true).when(r).isSePresentPing();
        doReturn(true).when(r).isSePresent();

        r.addObserver(getObs());
        r.startSeDetection(ObservableReader.PollingMode.SINGLESHOT);
        Thread.sleep(100);

        r.startRemovalSequence();
        Thread.sleep(100);

        Assert.assertEquals(WAIT_FOR_START_DETECTION, r.getCurrentMonitoringState());
    }

    /*
     * Helpers
     */

    static public BlankSmartPresenceTheadedReader getSmartSpy(String pluginName,
            String readerName) {
        BlankSmartPresenceTheadedReader r =
                Mockito.spy(new BlankSmartPresenceTheadedReader(pluginName, readerName, 1));
        return r;
    }

    static public BlankSmartPresenceTheadedReader getSmartPresenceMock(String pluginName,
            String readerName) {
        BlankSmartPresenceTheadedReader r = Mockito.mock(BlankSmartPresenceTheadedReader.class);
        doReturn("test").when(r).getName();
        return r;
    }


    static public ObservableReader.ReaderObserver getObs() {
        return new ObservableReader.ReaderObserver() {
            @Override
            public void update(ReaderEvent event) {}
        };
    }

    static public ObservableReader.ReaderObserver countDownOnTimeout(final CountDownLatch lock) {
        return new ObservableReader.ReaderObserver() {
            @Override
            public void update(ReaderEvent event) {
                if (ReaderEvent.EventType.TIMEOUT_ERROR.equals(event.getEventType())) {
                    lock.countDown();
                }
            }
        };
    }

}
