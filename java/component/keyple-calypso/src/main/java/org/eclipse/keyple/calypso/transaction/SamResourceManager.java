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
package org.eclipse.keyple.calypso.transaction;


import org.eclipse.keyple.calypso.transaction.exception.KeypleCalypsoNoSamResourceException;
import org.eclipse.keyple.calypso.transaction.exception.KeypleCalypsoSamResourceFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Management of SAM resources:
 * <p>
 * Provides methods fot the allocation/deallocation of SAM resources
 */
public class SamResourceManager {
    private static final Logger logger = LoggerFactory.getLogger(SamResourceManager.class);

    public enum AllocationMode {
        BLOCKING, NON_BLOCKING
    }

    /* the maximum time (in tenths of a second) during which the BLOCKING mode will wait */
    private static final int MAX_BLOCKING_TIME = 1000; // 1 sec
    private final HsmPlugin hsmPlugin;

    /**
     * Instantiate a new SamResourceManager with a HsmPlugin (e.g. HSM plugin).
     * <p>
     * The samReaderPlugin is used to retrieve the available SAM according to the provided filter.
     * <p>
     * 
     * @param hsmPlugin the plugin through which SAM readers are accessible
     * @param samReaderFilter the regular expression defining how to identify SAM readers among
     *        others.
     */
    public SamResourceManager(HsmPlugin hsmPlugin, String samReaderFilter) {
        this.hsmPlugin = hsmPlugin;
        logger.info("Create SAM resource manager from reader pool plugin: {}", hsmPlugin.getName());
    }

    /**
     * Allocate a SAM resource from the specified SAM group.
     * <p>
     * In the case where the allocation mode is BLOCKING, this method will wait until a SAM resource
     * becomes free and then return the reference to the allocated resource. However, the BLOCKING
     * mode will wait a maximum time defined in tenths of a second by MAX_BLOCKING_TIME.
     * <p>
     * In the case where the allocation mode is NON_BLOCKING and no SAM resource is available, this
     * method will return null.
     * <p>
     * If the samGroup argument is null, the first available SAM resource will be selected and
     * returned regardless of its group.
     *
     * @param allocationMode the blocking/non-blocking mode
     * @param samIdentifier the targeted SAM identifier
     * @return a SAM resource
     * @throws KeypleCalypsoNoSamResourceException if no SamResource is available
     * @throws KeypleCalypsoSamResourceFailureException if the allocation process failed (reader
     *         error or timeout)
     */
    public SamResource allocateSamResource(AllocationMode allocationMode,
            SamIdentifier samIdentifier)
            throws KeypleCalypsoNoSamResourceException, KeypleCalypsoSamResourceFailureException {
        long maxBlockingDate = System.currentTimeMillis() + MAX_BLOCKING_TIME;
        boolean noSamResourceLogged = false;
        logger.debug("Allocating SAM resource...");
        while (true) {
            try {
                SamResource samResource =
                        hsmPlugin.allocateSamResource(samIdentifier.getGroupReference());
                logger.debug("Allocation succeeded. SAM resource created.");
                return samResource;
            } catch (KeypleCalypsoNoSamResourceException e) {
                // ignore silently this exception
            }
            // loop until MAX_BLOCKING_TIME in blocking mode, only once in non-blocking mode
            if (allocationMode == AllocationMode.NON_BLOCKING) {
                logger.trace("No SAM resources available at the moment.");
                break;
            } else {
                if (!noSamResourceLogged) {
                    /* log once the first time */
                    logger.trace("No SAM resources available at the moment.");
                    noSamResourceLogged = true;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // set interrupt flag
                    logger.error("Interrupt exception in Thread.sleep.");
                }
                if (System.currentTimeMillis() >= maxBlockingDate) {
                    throw new KeypleCalypsoSamResourceFailureException(
                            "The allocation process failed. Timeout " + (MAX_BLOCKING_TIME / 1000.0)
                                    + " sec exceeded .");
                }
            }
        }
        throw new KeypleCalypsoNoSamResourceException("No SamResource available.");
    }

    /**
     * Free a previously allocated SAM resource.
     *
     * @param samResource the SAM resource reference to free
     * @throws KeypleCalypsoSamResourceFailureException if the deallocation process failed
     */
    public void freeSamResource(SamResource samResource)
            throws KeypleCalypsoSamResourceFailureException {
        // virtually infinite number of readers
        logger.debug("Freeing SAM resource.");
        hsmPlugin.releaseSamResource(samResource);
    }
}
