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
package org.eclipse.keyple.plugin.remotese.transport;


import org.eclipse.keyple.plugin.remotese.exception.KeypleRemoteException;
import org.eclipse.keyple.plugin.remotese.transport.model.KeypleDto;
import org.eclipse.keyple.plugin.remotese.transport.model.TransportDto;

/**
 * DtoNode is a one-point gateway for incoming and outgoing TransportDto. It extend DtoSender thus
 * sends KeypleDto and contains a DtoHandler for incoming KeypleDto
 */
public abstract class DtoNode implements DtoSender,DtoHandler {

    protected DtoSender dtoSender;

    public DtoNode(DtoSender dtoSender){
        this.dtoSender = dtoSender;
    }

    public void sendDTO(KeypleDto message) throws KeypleRemoteException {
        this.dtoSender.sendDTO(message);
    }

    public void sendDTO(TransportDto message) throws KeypleRemoteException {
        this.dtoSender.sendDTO(message);
    }

    public abstract TransportDto onDTO(TransportDto message);

    public String getNodeId(){
        return dtoSender.getNodeId();
    };
}
