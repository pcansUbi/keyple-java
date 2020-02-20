/********************************************************************************
 * Copyright (c) 2018 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.keyple.plugin.remotese.integration;


import java.util.HashMap;
import java.util.Map;
import org.eclipse.keyple.core.seproxy.SeProxyService;
import org.eclipse.keyple.core.seproxy.event.ObservableReader;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderException;
import org.eclipse.keyple.core.seproxy.protocol.TransmissionMode;
import org.eclipse.keyple.plugin.remotese.nativese.SlaveAPI;
import org.eclipse.keyple.plugin.remotese.pluginse.MasterAPI;
import org.eclipse.keyple.plugin.remotese.pluginse.VirtualObservableReader;
import org.eclipse.keyple.plugin.remotese.pluginse.VirtualReader;
import org.eclipse.keyple.plugin.remotese.transport.factory.ClientNode;
import org.eclipse.keyple.plugin.remotese.transport.factory.ServerNode;
import org.eclipse.keyple.plugin.remotese.transport.factory.TransportFactory;
import org.eclipse.keyple.plugin.remotese.transport.impl.java.LocalClient;
import org.eclipse.keyple.plugin.remotese.transport.impl.java.LocalTransportFactory;
import org.eclipse.keyple.plugin.stub.StubPlugin;
import org.eclipse.keyple.plugin.stub.StubReader;
import org.junit.*;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test Slave API methods : connectReader and DisconnectReader
 */
@RunWith(MockitoJUnitRunner.class)
public class SlaveAPITest {

    private static final Logger logger = LoggerFactory.getLogger(SlaveAPITest.class);

    @Rule
    public TestName name = new TestName();

    // Real objects
    TransportFactory factory;
    MasterAPI masterAPI;

    StubReader nativeReader;

    final String NATIVE_READER_NAME = "testStubReader";
    final String CLIENT_NODE_ID = "testClientNodeId";
    final String SERVER_NODE_ID = "testServerNodeId";
    final String REMOTE_SE_PLUGIN_NAME = "remoteseplugin1";


    final long RPC_TIMEOUT = 1000;

    // Spy Object
    SlaveAPI slaveAPI;

    @Before
    public void setTup() throws Exception {
        logger.info("------------------------------");
        logger.info("Test {}", name.getMethodName());
        logger.info("------------------------------");

        Assert.assertEquals(0, SeProxyService.getInstance().getPlugins().size());

        logger.info("*** Init LocalTransportFactory");
        // use a local transport factory for testing purposes (only java calls between client and
        // server). Only one client and one server bound together.
        factory = new LocalTransportFactory(SERVER_NODE_ID);

        logger.info("*** Bind Master Services");
        // bind Master services to server
        ServerNode serverNode = factory.getServer();
        masterAPI = Integration.createSpyMasterAPI(serverNode, REMOTE_SE_PLUGIN_NAME);
        serverNode.bindDtoNode(masterAPI);

        logger.info("*** Bind Slave Services");
        // bind Slave services to client
        ClientNode clientNode = factory.getClient(CLIENT_NODE_ID);
        slaveAPI = Integration.createSpySlaveAPI(clientNode, SERVER_NODE_ID);
        clientNode.bindDtoNode(slaveAPI);

        nativeReader =
                Integration.createStubReader(NATIVE_READER_NAME, TransmissionMode.CONTACTLESS);

    }


    @After
    public void tearDown() throws Exception {

        logger.info("TearDown Test");

        StubPlugin stubPlugin =
                (StubPlugin) SeProxyService.getInstance().getPlugin(Integration.SLAVE_STUB);

        Assert.assertEquals(0, ((ObservableReader) nativeReader).countObservers());

        // delete stubReader
        stubPlugin.unplugStubReader(nativeReader.getName(), true);

        Integration.unregisterAllPlugin(REMOTE_SE_PLUGIN_NAME);


        Assert.assertEquals(0, SeProxyService.getInstance().getPlugins().size());
    }



    /*
     * CONNECT METHOD
     */


    /**
     * Connect successfully a reader
     * 
     * @throws Exception
     */
    @Test
    public void testOKConnect() throws Exception {

        String sessionId = slaveAPI.connectReader(nativeReader);

        // assert that a virtual reader has been created
        VirtualObservableReader virtualReader = (VirtualObservableReader) masterAPI.getPlugin()
                .getReaderByRemoteName(NATIVE_READER_NAME, CLIENT_NODE_ID);

        Assert.assertEquals(NATIVE_READER_NAME, virtualReader.getNativeReaderName());
        Assert.assertEquals(1, nativeReader.countObservers());
        Assert.assertEquals(0, virtualReader.countObservers());
        Assert.assertNotNull(sessionId);

        // disconnect
        slaveAPI.disconnectReader("", nativeReader.getName());

    }

    /**
     * Connect successfully a reader with parameters
     *
     * @throws Exception
     */
    @Test
    public void testOKConnectWithParameter() throws Exception {

        String KEY = "keyTest";
        String VALUE = "valueTest";

        Map<String, String> options = new HashMap<String, String>();
        options.put(KEY, VALUE);

        String sessionId = slaveAPI.connectReader(nativeReader, options);

        // assert that a virtual reader has been created
        VirtualObservableReader virtualReader = (VirtualObservableReader) masterAPI.getPlugin()
                .getReaderByRemoteName(NATIVE_READER_NAME, CLIENT_NODE_ID);

        Assert.assertEquals(NATIVE_READER_NAME, virtualReader.getNativeReaderName());
        Assert.assertEquals(1, nativeReader.countObservers());
        Assert.assertEquals(0, virtualReader.countObservers());
        Assert.assertNotNull(sessionId);
        Assert.assertEquals(virtualReader.getParameters().get(KEY), VALUE);

        // disconnect
        slaveAPI.disconnectReader("", nativeReader.getName());
    }

    /**
     * Connect error : reader already exists
     * 
     * @throws Exception
     */
    @Test
    public void testKOConnectError() throws KeypleReaderException {

        // first connectReader is successful
        String sessionId = slaveAPI.connectReader(nativeReader);

        // should throw a DTO with an exception in master side KeypleReaderException
        try {
            slaveAPI.connectReader(nativeReader);
            // should ex be thrown
            Assert.fail();
        } catch (KeypleReaderException e) {

            // disconnect to cleanup
            slaveAPI.disconnectReader("", nativeReader.getName());
        }

    }

    /**
     * Connect error : impossible to send DTO
     * 
     * @throws Exception
     */
    @Test(expected = KeypleReaderException.class)
    public void testKOConnectServerError() throws Exception {

        // bind Slave to faulty client
        slaveAPI = Integration.createSpySlaveAPI(new LocalClient(CLIENT_NODE_ID, null),
                SERVER_NODE_ID);

        slaveAPI.connectReader(nativeReader);
        // should throw a KeypleRemoteException in slave side
    }

    /*
     * DISCONNECT METHOD
     */

    /**
     * Disconnect successfully a reader
     * 
     * @throws Exception
     */
    @Test
    public void testOKConnectDisconnect() throws Exception {

        // connect
        String sessionId = slaveAPI.connectReader(nativeReader);

        VirtualReader virtualReader = (VirtualReader) masterAPI.getPlugin()
                .getReaderByRemoteName(NATIVE_READER_NAME, CLIENT_NODE_ID);

        Assert.assertEquals(NATIVE_READER_NAME, virtualReader.getNativeReaderName());

        // disconnect
        slaveAPI.disconnectReader(sessionId, nativeReader.getName());

        // assert that the virtual reader has been destroyed
        Assert.assertEquals(0, masterAPI.getPlugin().getReaders().size());
    }


    /**
     * Disconnect Error : reader not connected
     * 
     * @throws Exception
     */
    // @Test
    // public void testKODisconnectNotFoundError() throws Exception {
    //
    // // assert an exception will be contained into keypleDto response
    // doAnswer(Integration.assertContainsException()).when(slaveAPI)
    // .onDTO(ArgumentMatchers.<TransportDto>any());
    //
    // // disconnect
    // slaveAPI.disconnectReader(nativeReader, CLIENT_NODE_ID);
    // // should throw exception in master side KeypleNotFound
    //
    // }


    /**
     * Disconnect error : impossible to send DTO
     * 
     * @throws Exception
     */
    @Test(expected = KeypleReaderException.class)
    public void testKODisconnectServerError() throws Exception {

        // bind Slave to faulty client
        slaveAPI = Integration.createSpySlaveAPI(new LocalClient(CLIENT_NODE_ID, null),
                SERVER_NODE_ID);

        slaveAPI.disconnectReader("null", nativeReader.getName());
        // should throw a KeypleRemoteException in slave side
    }

    @Test(expected = KeypleReaderException.class)
    public void testConnectTimeout() throws Exception {

        SlaveAPI slaveAPI = new SlaveAPI(SeProxyService.getInstance(), Integration.getFakeDtoNode(),
                SERVER_NODE_ID, RPC_TIMEOUT);

        // call the connect API of the slaveAPI
        slaveAPI.connectReader(nativeReader);

    }

    @Test(expected = KeypleReaderException.class)
    public void testDisconnectTimeout() throws Exception {

        SlaveAPI slaveAPI = new SlaveAPI(SeProxyService.getInstance(), Integration.getFakeDtoNode(),
                SERVER_NODE_ID, RPC_TIMEOUT);

        // call the disconnect API of the slaveAPI
        slaveAPI.disconnectReader("", "");

    }

}
