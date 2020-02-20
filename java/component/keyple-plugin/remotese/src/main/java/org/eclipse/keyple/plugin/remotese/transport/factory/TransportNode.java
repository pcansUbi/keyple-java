package org.eclipse.keyple.plugin.remotese.transport.factory;

import org.eclipse.keyple.plugin.remotese.transport.DtoNode;
import org.eclipse.keyple.plugin.remotese.transport.DtoSender;

/**
 * Bi-directional node, sends dto and pass them to DtoNode
 */
public interface TransportNode extends DtoSender {

    public void bindDtoNode(DtoNode dtoNode);

}
