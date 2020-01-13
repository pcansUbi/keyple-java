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
package org.eclipse.keyple.plugin.remotese.nativese.method;

import org.eclipse.keyple.core.seproxy.ReaderPoolPlugin;
import org.eclipse.keyple.core.seproxy.SeReader;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderAllocationException;
import org.eclipse.keyple.core.seproxy.message.SeResponse;
import org.eclipse.keyple.plugin.remotese.rm.IRemoteMethodExecutor;
import org.eclipse.keyple.plugin.remotese.rm.RemoteMethodName;
import org.eclipse.keyple.plugin.remotese.transport.json.JsonParser;
import org.eclipse.keyple.plugin.remotese.transport.model.KeypleDto;
import org.eclipse.keyple.plugin.remotese.transport.model.KeypleDtoHelper;
import org.eclipse.keyple.plugin.remotese.transport.model.TransportDto;
import com.google.gson.JsonObject;

public class RmPoolAllocateExecutor implements IRemoteMethodExecutor {

    ReaderPoolPlugin poolPlugin;

    @Override
    public RemoteMethodName getMethodName() {
        return RemoteMethodName.POOL_ALLOCATE_READER;
    }

    public RmPoolAllocateExecutor(ReaderPoolPlugin poolPlugin) {
        this.poolPlugin = poolPlugin;
    }

    @Override
    public TransportDto execute(TransportDto transportDto) {

        KeypleDto keypleDto = transportDto.getKeypleDTO();
        TransportDto out = null;
        SeResponse seResponse = null;

        // Extract info from keypleDto
        JsonObject body = JsonParser.getGson().fromJson(keypleDto.getBody(), JsonObject.class);
        String groupReference = body.get("groupReference").getAsString();
        try {
            // Execute Remote Method
            SeReader seReader = poolPlugin.allocateReader(groupReference);

            // Build Response
            JsonObject bodyResp = new JsonObject();
            bodyResp.addProperty("nativeReaderName", seReader.getName());
            bodyResp.addProperty("transmissionMode", seReader.getTransmissionMode().name());

            out = transportDto.nextTransportDTO(
                    KeypleDtoHelper.buildResponse(getMethodName().getName(), bodyResp.toString(),
                            null, seReader.getName(), null, keypleDto.getTargetNodeId(),
                            keypleDto.getRequesterNodeId(), keypleDto.getId()));
        } catch (KeypleReaderAllocationException e) {
            // if an exception occurs while allocating the reader, send it into a keypleDto to the
            // Master
            out = transportDto.nextTransportDTO(KeypleDtoHelper.ExceptionDTO(
                    getMethodName().getName(), e, null, null, null, keypleDto.getTargetNodeId(),
                    keypleDto.getRequesterNodeId(), keypleDto.getId()));
        }
        return out;
    }
}
