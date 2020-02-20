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
package org.eclipse.keyple.plugin.remotese.nativese;


import java.util.HashMap;
import java.util.Map;
import org.eclipse.keyple.core.seproxy.ReaderPlugin;
import org.eclipse.keyple.core.seproxy.ReaderPoolPlugin;
import org.eclipse.keyple.core.seproxy.SeProxyService;
import org.eclipse.keyple.core.seproxy.SeReader;
import org.eclipse.keyple.core.seproxy.event.ObservableReader;
import org.eclipse.keyple.core.seproxy.event.ReaderEvent;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderException;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderNotFoundException;
import org.eclipse.keyple.plugin.remotese.exception.KeypleRemoteException;
import org.eclipse.keyple.plugin.remotese.nativese.method.*;
import org.eclipse.keyple.plugin.remotese.rm.IRemoteMethodExecutor;
import org.eclipse.keyple.plugin.remotese.rm.RemoteMethodName;
import org.eclipse.keyple.plugin.remotese.rm.RemoteMethodTxEngine;
import org.eclipse.keyple.plugin.remotese.transport.*;
import org.eclipse.keyple.plugin.remotese.transport.json.JsonParser;
import org.eclipse.keyple.plugin.remotese.transport.model.KeypleDto;
import org.eclipse.keyple.plugin.remotese.transport.model.KeypleDtoHelper;
import org.eclipse.keyple.plugin.remotese.transport.model.TransportDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * SlaveAPI is the main component of the Remote Se Architecture on the slave side.
 * <p>
 * It allows also to connect/disconnect a reader trhough dedicated methods
 * <p>
 * It handles incoming {@link KeypleDto} and transfer them to the right {@link SeReader}
 * <p>
 * Configure the SlaveAPI with a {@link DtoNode} to enable communication with the
 * {@link org.eclipse.keyple.plugin.remotese.pluginse.MasterAPI}
 *
 */
public class SlaveAPI extends DtoNode implements ObservableReader.ReaderObserver,INativeReaderService {

    private static final Logger logger = LoggerFactory.getLogger(SlaveAPI.class);

    private final SeProxyService seProxyService;

    private final RemoteMethodTxEngine rmTxEngine;// rm command processor
    private final String masterNodeId;// master node id used for connect, disconnect, and events

    // used in case of a poolPlugin architecture
    private ReaderPoolPlugin readerPoolPlugin;

    public static final long DEFAULT_RPC_TIMEOUT = 10000;


    /**
     * Constructor with a default timeout DEFAULT_RPC_TIMEOUT
     * 
     * @param seProxyService : instance of the seProxyService
     * @param dtoSender : Define which DTO sender will be called when a DTO needs to be sent.
     * @param masterNodeId : Master Node Id to connect to
     */
    public SlaveAPI(SeProxyService seProxyService, DtoSender dtoSender, String masterNodeId) {
        super(dtoSender);
        this.seProxyService = seProxyService;
        this.rmTxEngine = new RemoteMethodTxEngine(dtoSender, DEFAULT_RPC_TIMEOUT);
        this.masterNodeId = masterNodeId;
        //this.bindDtoEndpoint(dtoSender);
    }

    /**
     * Constructor with custom timeout
     * 
     * @param seProxyService : instance of the seProxyService
     * @param dtoSender : Define which DTO sender will be called when a DTO needs to be sent.
     * @param masterNodeId : Master Node Id to connect to
     * @param timeout : timeout to be used before a request is abandonned
     */
    public SlaveAPI(SeProxyService seProxyService, DtoSender dtoSender, String masterNodeId,
            long timeout) {
        super(dtoSender);
        this.seProxyService = seProxyService;
        //this.dtoSender = dtoNode;
        this.rmTxEngine = new RemoteMethodTxEngine(dtoSender, timeout);
        this.masterNodeId = masterNodeId;
        //this.bindDtoEndpoint(dtoNode);
    }


    /**
     * HandleDTO from a DtoNode onDto() method will be called by the DtoNode
     * 
     * @param node : network entry point that receives DTO
     */
    //private void bindDtoEndpoint(DtoNode node) {
    //    node.setDtoHandler(this);// incoming traffic
    //}

    /**
     * Process and dispatch a {@link KeypleDto} to the right Native Reader. Override from the
     * interface {@link DtoHandler}
     * 
     * @param transportDto to be processed
     * @return Keyple DTO to be sent back
     */
    @Override
    public TransportDto onDTO(TransportDto transportDto) {

        KeypleDto keypleDTO = transportDto.getKeypleDTO();
        TransportDto out;

        logger.trace("{} onDto {}", this.dtoSender.getNodeId(), KeypleDtoHelper.toJson(keypleDTO));

        RemoteMethodName method = RemoteMethodName.get(keypleDTO.getAction());
        logger.trace("{} Remote Method called : {} - isRequest : {}", dtoSender.getNodeId(), method,
                keypleDTO.isRequest());

        switch (method) {

            /*
             * Response from Master
             */
            case READER_CONNECT:
            case READER_DISCONNECT:
                if (keypleDTO.isRequest()) {
                    throw new IllegalStateException("a " + keypleDTO.getAction()
                            + " request has been received by SlaveAPI");
                } else {
                    // send DTO to TxEngine
                    out = this.rmTxEngine.onDTO(transportDto);
                }
                break;

            /*
             * Request from Master
             */
            case READER_TRANSMIT:
                // must be a request
                if (keypleDTO.isRequest()) {
                    IRemoteMethodExecutor rmTransmit = new RmTransmitExecutor(this);
                    out = rmTransmit.execute(transportDto);
                } else {
                    throw new IllegalStateException(
                            "a READER_TRANSMIT response has been received by SlaveAPI");
                }
                break;

            case READER_TRANSMIT_SET:
                // must be a request
                if (keypleDTO.isRequest()) {
                    IRemoteMethodExecutor rmTransmitSet = new RmTransmitSetExecutor(this);
                    out = rmTransmitSet.execute(transportDto);
                } else {
                    throw new IllegalStateException(
                            "a READER_TRANSMIT_SET response has been received by SlaveAPI");
                }
                break;

            case DEFAULT_SELECTION_REQUEST:
                // must be a request
                if (keypleDTO.isRequest()) {
                    RmSetDefaultSelectionRequestExecutor rmSetDefaultSelectionRequest =
                            new RmSetDefaultSelectionRequestExecutor(this);
                    out = rmSetDefaultSelectionRequest.execute(transportDto);
                } else {
                    throw new IllegalStateException(
                            "a READER_TRANSMIT response has been received by SlaveAPI");
                }
                break;

            case POOL_ALLOCATE_READER:

                // must be a request
                if (keypleDTO.isRequest()) {
                    // executor
                    RmPoolAllocateExecutor rmPoolAllocateExecutor =
                            new RmPoolAllocateExecutor(this.readerPoolPlugin);
                    out = rmPoolAllocateExecutor.execute(transportDto);
                } else {
                    throw new IllegalStateException(
                            "a POOL_ALLOCATE_READER response has been received by SlaveAPI");
                }
                break;

            case POOL_RELEASE_READER:
                // must be a request
                if (keypleDTO.isRequest()) {
                    RmPoolReleaseExecutor rmPoolReleaseExecutor =
                            new RmPoolReleaseExecutor(this.readerPoolPlugin);
                    out = rmPoolReleaseExecutor.execute(transportDto);
                } else {
                    throw new IllegalStateException(
                            "a POOL_RELEASE_READER response has been received by SlaveAPI");
                }
                break;

            default:
                logger.warn("**** ERROR - UNRECOGNIZED ****");
                logger.warn("Receive unrecognized message action : {} {} {} {}",
                        keypleDTO.getAction(), keypleDTO.getSessionId(), keypleDTO.getBody(),
                        keypleDTO.isRequest());
                throw new IllegalStateException(
                        "a  ERROR - UNRECOGNIZED request has been received by SlaveAPI");
        }

        logger.trace("{} onDto response to be sent {}", dtoSender.getNodeId(),
                KeypleDtoHelper.toJson(out.getKeypleDTO()));

        return out;
    }


    /**
    
     */

    /**
     * Connect a local reader to Remote SE Plugin. Override from interface
     * {@link INativeReaderService}
     *
     * @param localReader : native reader to be connected
     * @return sessionId : if successful returns sessionId
     * @throws KeypleReaderException : if unsuccessful
     */
    @Override
    public String connectReader(SeReader localReader) throws KeypleReaderException {
        return connectReader(localReader, new HashMap<String, String>());
    }

    /**
     * Connect a local reader to Remote SE Plugin with options Override from interface
     * {@link INativeReaderService}
     *
     * @param localReader : native reader to be connected
     * @param options : options will be set as parameters of virtual reader
     */
    @Override
    public String connectReader(SeReader localReader, Map<String, String> options)
            throws KeypleReaderException {

        if (options == null) {
            options = new HashMap<String, String>();
        }

        logger.trace("{} connectReader {} from device {}", dtoSender.getNodeId(),
                localReader.getName(), dtoSender.getNodeId());

        RmConnectReaderTx connect = new RmConnectReaderTx(null, localReader.getName(), null,
                masterNodeId, localReader, dtoSender.getNodeId(), this, options);
        try {
            // blocking call
            return connect.execute(rmTxEngine);
        } catch (KeypleRemoteException e) {
            throw new KeypleReaderException("An error occurred while calling connectReader", e);
        }

    }

    /**
     * Disconnect a SeReader. Matching virtual session will be destroyed on Master node.
     *
     * @param sessionId (optional)
     * @param nativeReaderName local name of the reader, will be used coupled with the nodeId to
     *        identify the virtualReader
     * @throws KeypleReaderException if an error occured while sending remote command
     */
    @Override
    public void disconnectReader(String sessionId, String nativeReaderName)
            throws KeypleReaderException {
        logger.trace("{} disconnectReader {} from device {}", dtoSender.getNodeId(), nativeReaderName,
                dtoSender.getNodeId());

        RmDisconnectReaderTx disconnect = new RmDisconnectReaderTx(sessionId, nativeReaderName,
                dtoSender.getNodeId(), masterNodeId);

        try {
            // blocking call
            disconnect.execute(rmTxEngine);
            SeReader nativeReader = findLocalReader(nativeReaderName);
            if (nativeReader instanceof ObservableReader) {
                logger.trace("Disconnected reader is observable, removing slaveAPI observer");

                // stop propagating the local reader events
                ((ObservableReader) nativeReader).removeObserver(this);
            } else {
                logger.trace("Disconnected reader is not observable");
            }
        } catch (KeypleRemoteException e) {
            throw new KeypleReaderException("An error occurred while calling disconnectReader", e);
        } catch (KeypleReaderNotFoundException e) {
            logger.warn("SlaveAPI#disconnectReader() : reader with name was not found",
                    nativeReaderName);
        }
    }

    /**
     * Internal method to find a local reader by its name across multiple plugins
     * 
     * @param nativeReaderName : name of the reader to be found
     * @return Se Reader found if any
     * @throws KeypleReaderNotFoundException if not reader were found with this name
     */
    @Override
    public SeReader findLocalReader(String nativeReaderName) throws KeypleReaderNotFoundException {
        logger.trace("Find local reader by name {} in {} plugin(s)", nativeReaderName,
                seProxyService.getPlugins().size());
        for (ReaderPlugin plugin : seProxyService.getPlugins()) {
            try {
                return plugin.getReader(nativeReaderName);
            } catch (KeypleReaderNotFoundException e) {
                // continue
            }
        }
        throw new KeypleReaderNotFoundException(nativeReaderName);
    }

    /**
     * Do not call this method directly This method is called by a
     * Observable&lt;{@link ReaderEvent}&gt;
     * 
     * @param event event to be propagated to master device
     */
    @Override
    public void update(ReaderEvent event) {
        logger.trace("{} SlaveAPI - reader event {}", dtoSender.getNodeId(), event.getEventType());

        // construct json data
        String data = JsonParser.getGson().toJson(event);

        try {
            dtoSender.sendDTO(KeypleDtoHelper.buildNotification(
                    RemoteMethodName.READER_EVENT.getName(), data, null, event.getReaderName(),
                    null, this.dtoSender.getNodeId(), masterNodeId));
        } catch (KeypleRemoteException e) {
            logger.error("Event " + event.toString()
                    + " could not be sent though Remote Service Interface", e);
        }
    }


    public RemoteMethodTxEngine getRmTxEngine() {
        return rmTxEngine;
    }


    /**
     * Bind a ReaderPoolPlugin to the slaveAPI to enable Pool Plugins methods
     * 
     * @param readerPoolPlugin readerPoolPlugin to be used
     */
    public void registerReaderPoolPlugin(ReaderPoolPlugin readerPoolPlugin) {
        this.readerPoolPlugin = readerPoolPlugin;
        /**
         * Is this method necessary? Can't it be done by searching accross plugins?
         */
    }


}
