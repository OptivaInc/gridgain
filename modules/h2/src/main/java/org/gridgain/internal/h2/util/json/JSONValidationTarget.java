/*
 * Copyright 2004-2019 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.gridgain.internal.h2.util.json;

/**
 * JSON validation target.
 */
public abstract class JSONValidationTarget extends JSONTarget<JSONItemType> {

    /**
     * @return JSON item type of the top-level item, may not return
     *         {@link JSONItemType#VALUE}
     */
    @Override
    public abstract JSONItemType getResult();

}
