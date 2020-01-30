package org.eclipse.keyple.calypso.transaction;

import org.eclipse.keyple.calypso.command.po.AbstractPoCommandBuilder;

class PoVerifyPin extends PoCommand {
    public PoVerifyPin()  {
    }

    /**
     * @return true if the current command requires the split of the request
     */
    public boolean isSplitCommand() {
        return false;
    }

    /**
     * @return the identification of the command that requires the split of the request
     */
    public SplitCommandInfo getSplitCommandInfo() {
        return SplitCommandInfo.NOT_SET;
    }
}
