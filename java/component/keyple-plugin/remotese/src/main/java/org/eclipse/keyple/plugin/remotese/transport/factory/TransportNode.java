package org.eclipse.keyple.plugin.remotese.transport.factory;

import org.eclipse.keyple.plugin.remotese.transport.DtoNode;
import org.eclipse.keyple.plugin.remotese.transport.DtoSender;

/**
 * TransportNode is a component that sends Dto and also is the entry point for incoming dto. It transfer them to the dtoNode.
 */
public interface TransportNode extends DtoSender {

    public void bindDtoNode(DtoNode dtoNode);

}
