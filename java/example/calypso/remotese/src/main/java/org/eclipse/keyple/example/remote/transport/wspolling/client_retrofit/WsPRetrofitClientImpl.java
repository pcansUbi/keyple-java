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
package org.eclipse.keyple.example.remote.transport.wspolling.client_retrofit;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Observable;
import java.util.concurrent.TimeUnit;
import org.eclipse.keyple.example.remote.transport.wspolling.WsPTransportDTO;
import org.eclipse.keyple.plugin.remotese.transport.*;
import org.eclipse.keyple.plugin.remotese.transport.factory.ClientNode;
import org.eclipse.keyple.plugin.remotese.transport.model.KeypleDto;
import org.eclipse.keyple.plugin.remotese.transport.model.KeypleDtoHelper;
import org.eclipse.keyple.plugin.remotese.transport.model.TransportDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Rest client, polls server, based on client_retrofit and callbacks
 */
public class WsPRetrofitClientImpl extends Observable implements ClientNode {


    private static final Logger logger = LoggerFactory.getLogger(WsPRetrofitClientImpl.class);

    final private String baseUrl;
    final private String clientNodeId;
    final private String serverNodeId;
    private Boolean pollActivated;
    private Boolean isPolling;
    private ClientNode.ConnectCallback connectCallback;

    private DtoHandler dtoNode;


    public WsPRetrofitClientImpl(String baseUrl, String clientNodeId, String serverNodeId) {
        this.baseUrl = baseUrl;
        this.clientNodeId = clientNodeId;
        this.serverNodeId = serverNodeId;
        this.pollActivated = false;
        this.isPolling = false; // presume it will work
    }


    /**
     * recursive polling method based on client_retrofit callbacks
     * 
     * @param clientNodeId : terminal node Id (ie : androidDevice1)
     */
    private void startPollingWorker(final String clientNodeId) {
        this.pollActivated = true;
        poll(clientNodeId);
    }

    /**
     * Poll for keyple DTOs
     * 
     * @param clientNodeId
     */
    void poll(final String clientNodeId) {
        logger.trace("Polling from node {}", clientNodeId);
        final WsPRetrofitClientImpl thisClient = this;
        // if poll is activated
        if (this.pollActivated) {
            Call<KeypleDto> call = getRetrofitClient(baseUrl).getPolling(clientNodeId);
            call.enqueue(new Callback<KeypleDto>() {
                @Override
                public void onResponse(Call<KeypleDto> call, Response<KeypleDto> response) {

                    if (!isPolling) {
                        logger.trace("Polling state changed, polling is ON now");
                        setChanged();
                        notifyObservers(true);
                        isPolling = true;
                    }


                    int statusCode = response.code();

                    logger.trace("Polling for clientNodeId {} receive a httpResponse http code {}",
                            clientNodeId, statusCode);
                    if (statusCode == 200) {
                        processHttpResponseDTO(response);
                    } else {
                        // 204 : no response
                    }

                    // if a callback is set, call it
                    if (thisClient.connectCallback != null) {
                        thisClient.connectCallback.onConnectSuccess();
                    }

                    poll(clientNodeId);// recursive call to restart polling
                }

                @Override
                public void onFailure(Call<KeypleDto> call, Throwable t) {
                    logger.trace("Receive exception : {} , {}", t.getMessage(), t.getClass());

                    if (isPolling) {
                        logger.trace("Polling state changed, polling is OFF now");
                        setChanged();
                        notifyObservers(false);
                        isPolling = false;
                    }
                    // Log error here since request failed
                    if (t instanceof ConnectException) {
                        logger.error("Connection refused to server : {} , {}", t.getMessage(),
                                t.getCause());
                        thisClient.stopPollingWorker();

                        // if a callback is set, call it
                        if (thisClient.connectCallback != null) {
                            thisClient.connectCallback.onConnectFailure();
                        }
                    } else if (t instanceof SocketTimeoutException) {
                        logger.trace("polling ends by timeout, keep polling, error : {}",
                                t.getMessage());
                        poll(clientNodeId);// recursive call to restart polling
                    } else {
                        logger.error("Unexpected error when poll() : {} , {}", t.getMessage(),
                                t.getCause());

                        // if a callback is set, call it, will it be called?
                        if (thisClient.connectCallback != null) {
                            thisClient.connectCallback.onConnectFailure();
                        }
                        poll(clientNodeId);// recursive call to restart polling


                    }
                }
            });
        } else {
            logger.warn("poll is not active, call startPollingWorker to activate again");
            // poll is not active, call startPollingWorker to activate again
        }
    }


    private void stopPollingWorker() {
        this.pollActivated = false;
    }

    public Boolean getPollActivated() {
        return this.pollActivated;
    }

    public Boolean isPolling() {
        return this.isPolling;
    }


    private void processHttpResponseDTO(Response<KeypleDto> response) {

        KeypleDto responseDTO = response.body();

        if (!KeypleDtoHelper.isNoResponse(responseDTO)) {
            TransportDto transportDto = new WsPTransportDTO(responseDTO, this);
            // connection
            final TransportDto sendback = this.dtoNode.onDTO(transportDto);

            // if sendBack is not a noresponse (can be a keyple request or keyple response)
            if (!KeypleDtoHelper.isNoResponse(sendback.getKeypleDTO())) {
                // send the keyple object in a new thread to avoid blocking the polling

                sendDTO(sendback);

            }
        }


    }


    @Override
    public void sendDTO(TransportDto transportDto) {
        KeypleDto keypleDto = transportDto.getKeypleDTO();
        logger.trace("Ws Client send DTO {}", KeypleDtoHelper.toJson(keypleDto));

        if (KeypleDtoHelper.isNoResponse(transportDto.getKeypleDTO())) {
            // do not send a NoResponse keypleDto
        } else {
            // send it
            Call<KeypleDto> call = getRetrofitClient(baseUrl).postDto(keypleDto);

            // post Keyple DTO
            call.enqueue(new Callback<KeypleDto>() {

                // process response
                @Override
                public void onResponse(Call<KeypleDto> call, Response<KeypleDto> response) {
                    int statusCode = response.code();
                    logger.trace("Receive response from sendDto {} {}", clientNodeId, statusCode);
                    processHttpResponseDTO(response);
                }

                // process failure
                @Override
                public void onFailure(Call<KeypleDto> call, Throwable t) {
                    // Log error here since request failed
                    logger.trace("Receive failure from sendDto", t.getCause());
                    // startPollingWorker(nodeId);
                }
            });

        }
    }

    @Override
    public void sendDTO(KeypleDto message) {
        sendDTO(new WsPTransportDTO(message, null));
    }

    @Override
    public String getNodeId() {
        return this.clientNodeId;
    }

    @Override
    public String getServerNodeId() {
        return this.serverNodeId;
    }

    @Override
    public void bindDtoNode(DtoNode dtoNode) {
        this.dtoNode = dtoNode;
    }


    @Override
    public void connect(ConnectCallback connectCallback) {
        this.connectCallback = connectCallback;
        this.startPollingWorker(clientNodeId);
    }

    @Override
    public void disconnect() {
        this.stopPollingWorker();
    }

    static WsPRetrofitClient getRetrofitClient(String baseUrl) {

        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS).writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS).build();

        Retrofit retrofit = new Retrofit.Builder().client(okHttpClient).baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create()).build();

        return retrofit.create(WsPRetrofitClient.class);
    }

}
