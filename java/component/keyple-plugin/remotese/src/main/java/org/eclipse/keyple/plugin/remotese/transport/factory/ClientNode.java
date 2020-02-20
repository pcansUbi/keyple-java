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
package org.eclipse.keyple.plugin.remotese.transport.factory;


import org.eclipse.keyple.plugin.remotese.transport.DtoHandler;
import org.eclipse.keyple.plugin.remotese.transport.DtoNode;
import org.eclipse.keyple.plugin.remotese.transport.DtoSender;

/**
 * Client type of DtoNode, connects to a ServerNode
 */
public interface ClientNode extends TransportNode {

    /**
     * Connect to the server
     * 
     * @param connectCallback callback function
     */
    public abstract void connect(ConnectCallback connectCallback);

    /**
     * Disconnect from the server
     */
    public abstract void disconnect();

    /**
     * Retrieve ServerNodeId
     * 
     * @return server node id
     */
    public abstract String getServerNodeId();

    /**
     * Callback on the connection success
     */
    public interface ConnectCallback {
        /**
         * Called if the connection is sucessful
         */
        void onConnectSuccess();

        /**
         * Called if the connection has failed
         */
        void onConnectFailure();
    }

}
