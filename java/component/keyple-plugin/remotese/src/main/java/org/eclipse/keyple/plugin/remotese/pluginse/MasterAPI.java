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
package org.eclipse.keyple.plugin.remotese.pluginse;


import org.eclipse.keyple.core.seproxy.SeProxyService;
import org.eclipse.keyple.core.seproxy.SeReader;
import org.eclipse.keyple.core.seproxy.exception.KeyplePluginInstantiationException;
import org.eclipse.keyple.core.seproxy.exception.KeyplePluginNotFoundException;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderException;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderNotFoundException;
import org.eclipse.keyple.plugin.remotese.rm.RemoteMethodName;
import org.eclipse.keyple.plugin.remotese.transport.*;
import org.eclipse.keyple.plugin.remotese.transport.model.KeypleDto;
import org.eclipse.keyple.plugin.remotese.transport.model.KeypleDtoHelper;
import org.eclipse.keyple.plugin.remotese.transport.model.TransportDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Master API Create/Delete virtual reader based on Commands received from Slave API Propagates
 * Commands to SlaveAPI from virtual reader API
 *
 * Init this API with a {@link DtoSender} of your implementation. Link this API to one your
 * {@link DtoHandler}.
 *
 */
public class MasterAPI extends DtoNode {

    private static final Logger logger = LoggerFactory.getLogger(MasterAPI.class);

    private final RemoteSePluginImpl plugin;

    static public int PLUGIN_TYPE_DEFAULT = 0;
    static public int PLUGIN_TYPE_POOL = 1;

    private final int pluginType;

    public static final long DEFAULT_RPC_TIMEOUT = 10000;

    /**
     * Build a new MasterAPI, Entry point for incoming DTO in Master Manages RemoteSePlugin
     * lifecycle Manages Master Session Dispatch KeypleDTO
     *
     * @param seProxyService : SeProxyService
     * @param dtoSender : outgoing node to send Dto to Slave
     * @throws KeyplePluginInstantiationException if plugin does not instantiate
     */
    public MasterAPI(SeProxyService seProxyService, DtoSender dtoSender)
            throws KeyplePluginInstantiationException {
        this(seProxyService, dtoSender, DEFAULT_RPC_TIMEOUT);
    }

    /**
     * Build a new MasterAPI, Entry point for incoming DTO in Master Manages RemoteSePlugin
     * lifecycle Manages Master Session Dispatch KeypleDTO
     *
     * @param seProxyService : SeProxyService
     * @param dtoSender : outgoing node to send Dto to Slave
     * @param rpc_timeout : timeout in milliseconds to wait for an answer from slave before throwing
     *        an exception
     * @throws KeyplePluginInstantiationException if plugin does not instantiate
     */
    public MasterAPI(SeProxyService seProxyService, DtoSender dtoSender, long rpc_timeout)
            throws KeyplePluginInstantiationException {
        this(seProxyService, dtoSender, rpc_timeout, PLUGIN_TYPE_DEFAULT,
                RemoteSePlugin.DEFAULT_PLUGIN_NAME);
    }

    /**
     * Build a new MasterAPI, Entry point for incoming DTO in Master Manages RemoteSePlugin
     * lifecycle Manages Master Session Dispatch KeypleDTO
     *
     * @param seProxyService : SeProxyService
     * @param dtoSender : outgoing node to send Dto to Slave
     * @param rpcTimeout : timeout in milliseconds to wait for an answer from slave before throwing
     *        an exception
     * @param pluginType : either a default plugin or readerPool plugin, use
     *        {@link #PLUGIN_TYPE_DEFAULT} or @PLUGIN_TYPE_POOL
     * @param pluginName : specify a name for remoteseplugin
     * @throws KeyplePluginInstantiationException if plugin does not instantiate
     *
     * 
     */
    public MasterAPI(SeProxyService seProxyService, DtoSender dtoSender, long rpcTimeout,
            int pluginType, String pluginName) throws KeyplePluginInstantiationException {
        super(dtoSender);

        logger.info("Init MasterAPI with parameters {} {} {} {} {}", seProxyService, dtoSender,
                rpcTimeout, pluginType, pluginName);

        this.pluginType = pluginType;

        if (pluginName == null || pluginName.isEmpty()) {
            throw new IllegalArgumentException(
                    "pluginName should be properly defined (not null, not empty)");
        }

        // Instantiate Session Manager
        VirtualReaderSessionFactory sessionManager = new VirtualReaderSessionFactory();
        try {

            if (pluginType == PLUGIN_TYPE_DEFAULT) {
                /*
                 * // Instantiate Plugin this.plugin = new RemoteSePluginImpl(sessionManager,
                 * dtoNode, rpcTimeout, RemoteSePluginImpl.DEFAULT_PLUGIN_NAME);
                 */
                if (seProxyService.isRegistered(pluginName)) {
                    throw new IllegalArgumentException(
                            "plugin name is already registered to the platform : " + pluginName);
                }

                seProxyService.registerPlugin(
                        new RemoteSePluginFactory(sessionManager, dtoSender, rpcTimeout, pluginName));

                this.plugin = (RemoteSePluginImpl) seProxyService.getPlugin(pluginName);

            } else if (pluginType == PLUGIN_TYPE_POOL) {
                /*
                 * this.plugin = new RemoteSePoolPluginImpl(sessionManager, dtoNode, rpcTimeout,
                 * RemoteSePluginImpl.DEFAULT_PLUGIN_NAME + "_POOL");
                 */
                if (seProxyService.isRegistered(pluginName)) {
                    throw new IllegalArgumentException(
                            "plugin name is already registered to the platform : " + pluginName);
                }

                seProxyService.registerPlugin(new RemoteSePoolPluginFactory(sessionManager, dtoSender,
                        rpcTimeout, pluginName));

                this.plugin = (RemoteSePoolPluginImpl) seProxyService.getPlugin(pluginName);

            } else {
                throw new IllegalArgumentException(
                        "plugin type is not recognized, use static properties defined in MasterAPI#PLUGIN_TYPE_DEFAULT or MasterAPI#PLUGIN_TYPE_POOL");
            }
        } catch (KeyplePluginNotFoundException e) {
            throw new IllegalStateException(
                    "Unable to register plugin to platform : " + pluginName);
        }

        // Set this service as the Dto Handler for the node
        //this.bindDtoEndpoint(dtoNode);
    }

    /**
     * Set this service as the Dto Handler in your {@link DtoNode}
     * 
     * @param node : incoming Dto point
     */
//    private void bindDtoEndpoint(DtoNode node) {
  //      node.setDtoHandler(this);
    //}

    /**
     * Retrieve the Rse Plugin
     * 
     * @return the Remote Se Plugin managing the Virtual Readers
     */
    public RemoteSePlugin getPlugin() {
        return plugin;
    }

    /**
     * Handles incoming transportDTO
     * 
     * @param transportDto an incoming TransportDto (embeds a KeypleDto)
     * @return a Response transportDto (can be a NoResponse KeypleDto)
     */
    @Override
    public TransportDto onDTO(TransportDto transportDto) {

        KeypleDto keypleDTO = transportDto.getKeypleDTO();
        RemoteMethodName method = RemoteMethodName.get(keypleDTO.getAction());
        logger.trace("onDTO, Remote Method called : {} - isRequest : {} - keypleDto : {}", method,
                keypleDTO.isRequest(), KeypleDtoHelper.toJson(keypleDTO));


        switch (method) {

            /*
             * Requests from slave
             */
            case READER_CONNECT:
                if (keypleDTO.isRequest()) {
                    return new RmConnectReaderExecutor(this.plugin, this.dtoSender)
                            .execute(transportDto);
                } else {
                    throw new IllegalStateException(
                            "a READER_CONNECT response has been received by MasterAPI");
                }
            case READER_DISCONNECT:
                if (keypleDTO.isRequest()) {
                    return new RmDisconnectReaderExecutor(this.plugin).execute(transportDto);
                } else {
                    throw new IllegalStateException(
                            "a READER_DISCONNECT response has been received by MasterAPI");
                }

                /*
                 * Notifications from slave
                 */

            case READER_EVENT:
                // process response with the Event Reader RmMethod
                return new RmReaderEventExecutor(plugin).execute(transportDto);

            /*
             * Response from slave
             */

            case READER_TRANSMIT:
            case READER_TRANSMIT_SET:
            case DEFAULT_SELECTION_REQUEST:
                if (keypleDTO.isRequest()) {
                    throw new IllegalStateException("a " + keypleDTO.getAction()
                            + " request has been received by MasterAPI");
                }

                // dispatch dto to the appropriate reader
                try {
                    // find reader by sessionId
                    VirtualReaderImpl reader = getReaderBySessionId(keypleDTO.getSessionId());

                    // process response with the reader rmtx engine
                    return reader.getRmTxEngine().onDTO(transportDto);

                } catch (KeypleReaderNotFoundException e) {
                    // reader not found;
                    throw new IllegalStateException(
                            "Virtual Reader was not found while receiving a "
                                    + keypleDTO.getAction() + " response",
                            e);
                } catch (KeypleReaderException e) {
                    // reader not found;
                    throw new IllegalStateException("Readers list has not been initiated", e);
                }

            case POOL_ALLOCATE_READER:
            case POOL_RELEASE_READER:
                if (keypleDTO.isRequest()) {
                    throw new IllegalStateException("a " + keypleDTO.getAction()
                            + " request has been received by MasterAPI");
                }
                if (pluginType != PLUGIN_TYPE_POOL) {
                    throw new IllegalStateException("a " + keypleDTO.getAction()
                            + " request has been received by MasterAPI but plugin is not pool compatible");
                }

                /*
                 * dispatch message to plugin
                 */
                return ((RemoteSePoolPluginImpl) plugin).getRmTxEngine().onDTO(transportDto);

            default:
                logger.error("Receive a KeypleDto with no recognised action");
                return transportDto.nextTransportDTO(KeypleDtoHelper.NoResponse(keypleDTO.getId()));
        }
    }


    /**
     * Retrieve reader by its session Id
     * 
     * @param sessionId : sessionId which virtual reader is tight to
     * @return VirtualReader matching the sessionId
     * @throws KeypleReaderNotFoundException : if none reader was found
     */
    private VirtualReaderImpl getReaderBySessionId(String sessionId) throws KeypleReaderException {
        for (SeReader reader : plugin.getReaders()) {

            if (((VirtualReaderImpl) reader).getSession().getSessionId().equals(sessionId)) {
                return (VirtualReaderImpl) reader;
            }
        }
        throw new KeypleReaderNotFoundException(
                "Reader session was not found for session : " + sessionId);
    }

}
